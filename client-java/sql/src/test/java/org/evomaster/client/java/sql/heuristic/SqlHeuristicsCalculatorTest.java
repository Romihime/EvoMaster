package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.QueryResultSet;
import org.evomaster.client.java.sql.VariableDescriptor;
import org.evomaster.client.java.sql.internal.SqlDistanceWithMetrics;
import org.evomaster.client.java.sql.internal.SqlParserUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.evomaster.client.java.sql.heuristic.SqlHeuristicsCalculator.TRUE_TRUTHNESS;
import static org.junit.jupiter.api.Assertions.*;

import net.sf.jsqlparser.schema.Column;


public class SqlHeuristicsCalculatorTest {

    @Test
    public void testSelectFromTableWithRows() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM Person";

        QueryResult databaseContents = new QueryResult(Collections.singletonList("name"), "Person");
        databaseContents.addRow(new DataRow("name", "John", "Person"));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, databaseContents);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));
        assertEquals(true, heuristicResult.getTruthness().isTrue());
        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "person"), queryResult.seeVariableDescriptors().get(0));
    }

    @Test
    public void testSelectWithFalseWhereConditionWithoutFrom() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT 1 AS example_column WHERE 1 = 0";
        QueryResult contents = new QueryResult(Collections.singletonList("example_column"), null);
        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, contents);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        double expectedOfTrue = TruthnessUtils.buildAndAggregationTruthness(TRUE_TRUTHNESS, new Truthness(SqlHeuristicsCalculator.C, 1d)).getOfTrue();
        assertEquals(expectedOfTrue, heuristicResult.getTruthness().getOfTrue());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("example_column", "example_column", null), queryResult.seeVariableDescriptors().get(0));

    }


    @Test
    public void testSelectNoFromNeitherWhereClauses() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT 1 AS example_column";
        QueryResult virtualTableContents = new QueryResult(Collections.singletonList("example_column"), null);
        virtualTableContents.addRow(new DataRow("example_column", 1, null));
        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, virtualTableContents);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));
        assertEquals(true, heuristicResult.getTruthness().isTrue());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("example_column", "example_column", null), queryResult.seeVariableDescriptors().get(0));
    }


    @Test
    public void testSelectFromEmptyTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM Person";
        QueryResult contents = new QueryResult(Collections.singletonList("name"), "Person");
        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, contents);

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));
        assertEquals(false, heuristicResult.getTruthness().isTrue());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "person"), queryResult.seeVariableDescriptors().get(0));

        assertEquals(0, queryResult.seeRows().size());
    }

    @Test
    public void testLeftJoinWithRowsInLeftTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA LEFT JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, leftTable, rightTable);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));
        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
    }


    @Test
    public void testRightJoinWithRowsInRightTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA RIGHT JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        rightTable.addRow(new DataRow("name", "John", "TableB"));
        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, leftTable, rightTable);

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));
        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        /*
         * Since name is both tableA.name and tableB.name exist,
         * the "name" column is resolved as tableA.name instead
         * of tableB.name.
         */
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals(null, heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));

    }

    @Test
    public void testRightJoinWithRowsInLeftTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA RIGHT JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTables = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));
        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, leftTable, rightTables);

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));
        assertEquals(false, heuristicResult.getTruthness().isTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(0, heuristicResult.getQueryResult().seeRows().size());
    }

    @Test
    public void testLeftJoinWithRowsInRightTable() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM TableA LEFT JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        rightTable.addRow(new DataRow("name", "John", "TableB"));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, leftTable, rightTable);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));
        assertEquals(false, heuristicResult.getTruthness().isTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(0, heuristicResult.getQueryResult().seeRows().size());
    }

    @Test
    public void testLeftOuterJoinWithRowsInRightTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA LEFT OUTER JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        rightTable.addRow(new DataRow("name", "John", "TableB"));
        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, leftTable, rightTable);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        double expectedOfTrue = SqlHeuristicsCalculator.C;
        assertEquals(expectedOfTrue, heuristicResult.getTruthness().getOfTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(0, heuristicResult.getQueryResult().seeRows().size());
    }

    @Test
    public void testRightOuterJoinWithRowsInLeftTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA RIGHT OUTER JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));
        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, leftTable, rightTable);

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        double expectedOfTrue = SqlHeuristicsCalculator.C;
        assertEquals(expectedOfTrue, heuristicResult.getTruthness().getOfTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(0, heuristicResult.getQueryResult().seeRows().size());
    }

    @Test
    public void testLeftOuterJoinWithRowsInLeftTable() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM TableA LEFT OUTER JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));
        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, leftTable, rightTable);

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));

    }


    @Test
    public void testRightOuterJoinWithRowsInRightTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA RIGHT OUTER JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        rightTable.addRow(new DataRow("name", "John", "TableB"));
        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, leftTable, rightTable);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals(null, heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));


    }

    @Test
    public void testCrossJoinWithEmptyTables() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM TableA CROSS JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, leftTable, rightTable);
        double expectedOfTrue = SqlHeuristicsCalculator.C;

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(expectedOfTrue, heuristicResult.getTruthness().getOfTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(0, heuristicResult.getQueryResult().seeRows().size());

    }

    @Test
    public void testCrossJoinWithRows() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM TableA CROSS JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));
        rightTable.addRow(new DataRow("name", "Jack", "TableB"));
        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, leftTable, rightTable);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
    }

    private static ColumnDto createColumnDto(String columnName) {
        ColumnDto column = new ColumnDto();
        column.name = columnName;
        return column;
    }

    private static TableDto createTableDto(String tableName) {
        TableDto table = new TableDto();
        table.name = tableName;
        return table;
    }

    @Test
    public void testInnerJoin() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT Employees.name, Departments.department_name\n" +
                "FROM Employees\n" +
                "JOIN Departments ON Employees.department_id = Departments.department_id";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id"), "employees");
        employees.addRow(Arrays.asList("name", "department_id"), "employees", Arrays.asList("John", 1));

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, employees, departments);

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "employees"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
        assertEquals("Sales", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("department_name"));
    }

    @Test
    public void testManyInnerJoins() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT Employees.name, Departments.department_name, Projects.project_name\n" +
                "FROM Employees\n" +
                "JOIN Departments ON Employees.department_id = Departments.department_id\n" +
                "JOIN Projects ON Employees.project_id = Projects.project_id";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id", "project_id"), "employees");
        employees.addRow(Arrays.asList("name", "department_id", "project_id"), "employees", Arrays.asList("John", 1, 1));

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));

        QueryResult projects = new QueryResult(Arrays.asList("project_id", "project_name"), "projects");
        projects.addRow(Arrays.asList("project_id", "project_name"), "projects", Arrays.asList(1, "ProjectX"));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, employees, departments, projects);

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(3, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "employees"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));
        assertEquals(new VariableDescriptor("project_name", "project_name", "projects"), heuristicResult.getQueryResult().seeVariableDescriptors().get(2));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
        assertEquals("Sales", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("department_name"));
        assertEquals("ProjectX", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("project_name"));


    }

    @Test
    public void testUnion() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM Employees " +
                "UNION " +
                "SELECT department_name AS name FROM Departments ";
        QueryResult employees = new QueryResult(Collections.singletonList("name"), "Employees");
        employees.addRow(Collections.singletonList("name"), "Employees", Collections.singletonList("John"));

        QueryResult departments = new QueryResult(Collections.singletonList("department_name"), "Departments");
        departments.addRow(Collections.singletonList("department_name"), "Departments", Collections.singletonList("Sales"));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, employees, departments);

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", null), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(2, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
        assertEquals("Sales", heuristicResult.getQueryResult().seeRows().get(1).getValueByName("name"));

    }

    @Test
    public void testCrossJoin() {
        final DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT Employees.name, Departments.department_name\n" +
                "FROM Employees\n" +
                "CROSS JOIN Departments";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id"), "employees");
        employees.addRow(Arrays.asList("name", "department_id"), "employees", Arrays.asList("John", 1));

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, employees, departments);

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "employees"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
        assertEquals("Sales", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("department_name"));


    }

    private static @NotNull DbInfoDto buildSchema() {
        DbInfoDto schema = new DbInfoDto();

        TableDto employeesTable = createTableDto("Employees");
        employeesTable.columns.add(createColumnDto("name"));
        employeesTable.columns.add(createColumnDto("first_name"));
        employeesTable.columns.add(createColumnDto("department_id"));
        employeesTable.columns.add(createColumnDto("project_id"));
        employeesTable.columns.add(createColumnDto("salary"));

        TableDto departmentsTable = createTableDto("Departments");
        departmentsTable.columns.add(createColumnDto("department_id"));
        departmentsTable.columns.add(createColumnDto("department_name"));

        TableDto projectsTable = createTableDto("Projects");
        projectsTable.columns.add(createColumnDto("project_id"));
        projectsTable.columns.add(createColumnDto("project_name"));


        TableDto tableA = createTableDto("TableA");
        tableA.columns.add(createColumnDto("name"));

        TableDto tableB = createTableDto("TableB");
        tableA.columns.add(createColumnDto("name"));

        TableDto personTable = createTableDto("Person");
        personTable.columns.add(createColumnDto("name"));
        personTable.columns.add(createColumnDto("age"));
        personTable.columns.add(createColumnDto("salary"));

        TableDto categoriesTable = createTableDto("Categories");
        categoriesTable.columns.add(createColumnDto("id"));
        categoriesTable.columns.add(createColumnDto("name"));
        categoriesTable.columns.add(createColumnDto("parent_id"));

        schema.tables.add(employeesTable);
        schema.tables.add(departmentsTable);
        schema.tables.add(projectsTable);
        schema.tables.add(tableA);
        schema.tables.add(tableB);
        schema.tables.add(personTable);
        schema.tables.add(categoriesTable);

        return schema;
    }

    @Test
    public void testLeftJoin() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT Employees.name, Departments.department_name\n" +
                "FROM Employees\n" +
                "LEFT JOIN Departments ON Employees.department_id = Departments.department_id";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id"), "employees");
        employees.addRow(Arrays.asList("name", "department_id"), "employees", Arrays.asList("John", 1));

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, employees, departments);

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "employees"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
        assertEquals(null, heuristicResult.getQueryResult().seeRows().get(0).getValueByName("department_name"));

    }

    @Test
    public void testRightJoin() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT Employees.name, Departments.department_name\n" +
                "FROM Employees\n" +
                "RIGHT JOIN Departments ON Employees.department_id = Departments.department_id";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id"), "employees");

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, employees, departments);

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "employees"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals(null, heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
        assertEquals("Sales", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("department_name"));

    }

    @Test
    public void testSelectFromSubquery() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM (SELECT name FROM Employees) AS Subquery";
        QueryResult employees = new QueryResult(Collections.singletonList("name"), "Employees");
        employees.addRow(new DataRow("name", "John", "Employees"));

        QueryResult[] arrayOfQueryResultSet = {employees};
        Select parsedSqlCommand = (Select) SqlParserUtils.parseSqlCommand(sqlCommand);

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, arrayOfQueryResultSet);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic(parsedSqlCommand);

        assertTrue(heuristicResult.getTruthness().isTrue());
        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", null, null), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));

    }


    @Test
    public void testSelectWithAlias() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT first_name AS name, salary AS income\n" +
                "    FROM Employees" +
                "    WHERE income > 100";

        List<VariableDescriptor> variableDescriptors = new ArrayList<>();
        variableDescriptors.add(new VariableDescriptor("first_name", "name", "employees"));
        variableDescriptors.add(new VariableDescriptor("salary", "income", "employees"));
        QueryResult employees = new QueryResult(variableDescriptors);
        employees.addRow(new DataRow(variableDescriptors, Arrays.asList("John", 10000)));

        QueryResult[] arrayOfQueryResultSet = {employees};
        Select parsedSqlCommand = (Select) SqlParserUtils.parseSqlCommand(sqlCommand);

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, arrayOfQueryResultSet);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic(parsedSqlCommand);

        assertTrue(heuristicResult.getTruthness().isTrue());

        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("first_name", "name", "employees"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("salary", "income", "employees"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
        assertEquals(10000, heuristicResult.getQueryResult().seeRows().get(0).getValueByName("income"));
    }


    @Test
    public void testSelectFromSubqueryWithAlias() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name, income\n" +
                "FROM (\n" +
                "    SELECT first_name AS name, salary AS income\n" +
                "    FROM Employees\n" +
                ") AS subquery\n" +
                "WHERE income > 100";

        QueryResult employees = new QueryResult(Arrays.asList("first_name", "salary"), "employees");
        employees.addRow(new DataRow("employees", Arrays.asList("first_name", "salary"), Arrays.asList("John", 10000)));

        Select select = (Select) SqlParserUtils.parseSqlCommand(sqlCommand);

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(employees);
        TableColumnResolver tableColumnResolver = new TableColumnResolver(schema);

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(tableColumnResolver, null, queryResultSet);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic(select);

        assertTrue(heuristicResult.getTruthness().isTrue());

        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", null), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("income", "income", null), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());

        Table subqueryTable = new Table();
        subqueryTable.setName("subquery");

        Column nameColumn = new Column();
        nameColumn.setTable(subqueryTable);
        nameColumn.setColumnName("name");

        Column incomeColumn = new Column();
        incomeColumn.setTable(subqueryTable);
        incomeColumn.setColumnName("income");

        tableColumnResolver.enterStatementeContext(select);

        SqlColumnReference nameSqlColumnReference = tableColumnResolver.resolve(nameColumn);
        assertTrue(nameSqlColumnReference.getTableReference() instanceof SqlDerivedTableReference);

        SqlColumnReference incomeSqlColumnReference = tableColumnResolver.resolve(incomeColumn);
        assertTrue(incomeSqlColumnReference.getTableReference() instanceof SqlDerivedTableReference);

        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName(nameSqlColumnReference.getColumnName()));
        assertEquals(10000, heuristicResult.getQueryResult().seeRows().get(0).getValueByName(incomeSqlColumnReference.getColumnName()));

        tableColumnResolver.exitCurrentStatementContext();
    }

    @Test
    public void testInnerJoinWithAliases() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT e.name, d.department_name\n" +
                "FROM Employees e\n" +
                "JOIN Departments d ON e.department_id = d.department_id";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id"), "employees");
        employees.addRow(Arrays.asList("name", "department_id"), "employees", Arrays.asList("John", 1));

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, employees, departments);
        SqlDistanceWithMetrics distanceWithMetrics = calculator.computeDistance(sqlCommand);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testInnerJoinWithSubqueries() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT e.name, d.department_name\n" +
                "FROM (SELECT name, department_id FROM Employees) e\n" +
                "JOIN (SELECT department_id, department_name FROM Departments) d ON e.department_id = d.department_id";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id"), "employees");
        employees.addRow(Arrays.asList("name", "department_id"), "employees", Arrays.asList("John", 1));

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, employees, departments);

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());

        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", null), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", null), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
        assertEquals("Sales", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("department_name"));

    }


    @Test
    public void testDelete() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "DELETE FROM departments WHERE department_id=2";

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(2, "Marketing"));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, departments);

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Delete) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());

        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("department_id", "department_id", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));


    }

    @Test
    public void testUpdate() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "UPDATE departments SET department_name='Telemarketing' WHERE department_name='Marketing'";

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(2, "Marketing"));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, departments);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Update) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());
        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("department_id", "department_id", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));
    }

    @Test
    public void testFailOnInsert() {
        DbInfoDto schema = buildSchema();

        final String sqlCommand = "INSERT INTO departments (department_id,department_name) VALUES (3,'Telemarketing')";

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(2, "Marketing"));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, departments);

        assertThrows(IllegalArgumentException.class, () -> calculator.computeDistance(sqlCommand));
    }

    @Test
    public void testDeleteNoWhere() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "DELETE FROM departments";

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, departments);

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Delete) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());

        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("department_id", "department_id", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().get(0).getValueByName("department_id"));
        assertEquals("Sales", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("department_name"));
    }

    @Test
    public void testSelectNoFromNoWhere() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT 24";

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());
        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor(null, null, null), queryResult.seeVariableDescriptors().get(0));

        assertEquals(1, queryResult.seeRows().size());
        assertEquals(24, ((Number)queryResult.seeRows().get(0).getValueByName(null)).intValue());

    }

    @Test
    public void testUnionSelectNoFromNoWhere() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT 24 UNION SELECT 42";

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));
        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor(null, null, null), queryResult.seeVariableDescriptors().get(0));

        assertEquals(2, queryResult.seeRows().size());
        assertEquals(24, ((Number)queryResult.seeRows().get(0).getValueByName(null)).intValue());
        assertEquals(42, ((Number)queryResult.seeRows().get(1).getValueByName(null)).intValue());
    }

    @Test
    public void testSelectFromTableWithRowsNoWhere() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM Person";

        QueryResult databaseContents = new QueryResult(Arrays.asList("name", "age", "salary"), "Person");
        databaseContents.addRow(new DataRow("Person", Arrays.asList("name", "age", "salary"), Arrays.asList("John", 30, 50000)));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, databaseContents);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));


        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "person"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));


        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        DataRow row = heuristicResult.getQueryResult().seeRows().get(0);
        assertEquals("John", row.getValueByName("name"));
    }


    @Test
    public void testSelectFromTableWithRowsNoWhereUsingAlias() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name AS person_name FROM Person";

        QueryResult databaseContents = new QueryResult(Arrays.asList("name", "age", "salary"), "Person");
        databaseContents.addRow(new DataRow("Person", Arrays.asList("name", "age", "salary"), Arrays.asList("John", 30, 50000)));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, databaseContents);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "person_name", "person"),
                queryResult.seeVariableDescriptors().iterator().next());

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        DataRow row = heuristicResult.getQueryResult().seeRows().get(0);
        assertEquals("John", row.getValueByName("person_name"));
    }

    @Test
    public void testSelectAllFromTable() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT * FROM Person";

        QueryResult databaseContents = new QueryResult(Arrays.asList("name", "age", "salary"), "Person");
        databaseContents.addRow(new DataRow("Person", Arrays.asList("name", "age", "salary"), Arrays.asList("John", 30, 50_000)));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, databaseContents);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(3, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "person"),
                queryResult.seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("age", "age", "person"),
                queryResult.seeVariableDescriptors().get(1));
        assertEquals(new VariableDescriptor("salary", "salary", "person"),
                queryResult.seeVariableDescriptors().get(2));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        DataRow row = heuristicResult.getQueryResult().seeRows().get(0);
        assertEquals("John", row.getValueByName("name"));
        assertEquals(30, row.getValueByName("age"));
        assertEquals(50_000, row.getValueByName("salary"));
    }

    @Test
    public void testSelectAllFromSubquery() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT * FROM (SELECT salary, age FROM (SELECT * FROM person))";

        QueryResult databaseContents = new QueryResult(Arrays.asList("name", "age", "salary"), "person");
        databaseContents.addRow(new DataRow("Person", Arrays.asList("name", "age", "salary"), Arrays.asList("John", 30, 50_000)));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, databaseContents);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(2, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("salary", "salary", null),
                queryResult.seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("age", "age", null),
                queryResult.seeVariableDescriptors().get(1));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        DataRow row = heuristicResult.getQueryResult().seeRows().get(0);
        assertEquals(30, row.getValueByName("age"));
        assertEquals(50_000, row.getValueByName("salary"));
    }


    @Test
    public void testSelfJoin() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT\n" +
                "  child.name AS category,\n" +
                "  parent.name AS parent_category\n" +
                "FROM categories child\n" +
                "LEFT JOIN categories parent ON child.parent_id = parent.id;\n";

        QueryResult databaseContents = new QueryResult(Arrays.asList("id", "name", "parent_id"), "categories");
        databaseContents.addRow(new DataRow("categories", Arrays.asList("id", "name", "parent_id"), Arrays.asList(1, "Electronics", null)));
        databaseContents.addRow(new DataRow("categories", Arrays.asList("id", "name", "parent_id"), Arrays.asList(2, "Computers", 1)));
        databaseContents.addRow(new DataRow("categories", Arrays.asList("id", "name", "parent_id"), Arrays.asList(3, "Laptops", 2)));
        databaseContents.addRow(new DataRow("categories", Arrays.asList("id", "name", "parent_id"), Arrays.asList(4, "Phones", 1)));
        databaseContents.addRow(new DataRow("categories", Arrays.asList("id", "name", "parent_id"), Arrays.asList(5, "Accessories", 2)));

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, databaseContents);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(2, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "category", "categories"),
                queryResult.seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("name", "parent_category", "categories"),
                queryResult.seeVariableDescriptors().get(1));

        final List<DataRow> dataRows = heuristicResult.getQueryResult().seeRows();
        assertEquals(5, dataRows.size());
        assertEquals("Electronics", dataRows.get(0).getValueByName("category"));
        assertEquals(null, dataRows.get(0).getValueByName("parent_category"));

        assertEquals("Computers", dataRows.get(1).getValueByName("category"));
        assertEquals("Electronics", dataRows.get(1).getValueByName("parent_category"));

        assertEquals("Laptops", dataRows.get(2).getValueByName("category"));
        assertEquals("Computers", dataRows.get(2).getValueByName("parent_category"));

        assertEquals("Phones", dataRows.get(3).getValueByName("category"));
        assertEquals("Electronics", dataRows.get(3).getValueByName("parent_category"));

        assertEquals("Accessories", dataRows.get(4).getValueByName("category"));
        assertEquals("Computers", dataRows.get(4).getValueByName("parent_category"));

    }

    @Test
    public void testLeftJoinWithTable() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT\n" +
                "  e.*\n" +
                "FROM employees e\n" +
                "LEFT JOIN projects p ON e.project_id = p.project_id;\n";

        QueryResult employees = new QueryResult(Arrays.asList("name", "first_name", "department_id", "project_id", "salary"), "employees");
        employees.addRow(Arrays.asList("name", "first_name", "department_id", "project_id", "salary"), "employees", Arrays.asList("John Doe", "John", null, 1, 50_000));

        QueryResult projects = new QueryResult(Arrays.asList("project_id", "project_name"), "projects");
        projects.addRow(Arrays.asList("project_id", "project_name"), "projects", Arrays.asList(1, "ProjectX"));



        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(schema, null, employees, projects);
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(5, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "employees", "e"),
                queryResult.seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("first_name", "first_name", "employees", "e"),
                queryResult.seeVariableDescriptors().get(1));
        assertEquals(new VariableDescriptor("department_id", "department_id", "employees", "e"),
                queryResult.seeVariableDescriptors().get(2));
        assertEquals(new VariableDescriptor("project_id", "project_id", "employees", "e"),
                queryResult.seeVariableDescriptors().get(3));
        assertEquals(new VariableDescriptor("salary", "salary", "employees", "e"),
                queryResult.seeVariableDescriptors().get(4));

        final List<DataRow> dataRows = heuristicResult.getQueryResult().seeRows();
        assertEquals(1, dataRows.size());
        assertEquals("John Doe", dataRows.get(0).getValueByName("name"));
        assertEquals("John", dataRows.get(0).getValueByName("first_name"));
        assertEquals(null, dataRows.get(0).getValueByName("department_id"));
        assertEquals(1, dataRows.get(0).getValueByName("project_id"));
        assertEquals(50_000, dataRows.get(0).getValueByName("salary"));

    }


}
