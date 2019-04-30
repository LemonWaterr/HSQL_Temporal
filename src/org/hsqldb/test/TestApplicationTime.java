package org.hsqldb.test;

import org.hsqldb.HsqlException;

import java.sql.*;

public class TestApplicationTime extends TestBase{

    Connection conn;
    private static String dbPath = "/hsql/testApptime";
    Statement         stmt;

    public TestApplicationTime(String testName) {
        super(testName, "jdbc:hsqldb:file:" + dbPath, false, false);
        TestUtil.deleteDatabase(dbPath);
    }

    private void openConnection() throws SQLException {
        conn = newConnection();
    }

    public void setUp() throws Exception {

        super.setUp();

        try {
            openConnection();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void tearDown() {

        try {
            stmt.execute("DROP TABLE Emp");
            conn.close();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }

        super.tearDown();
    }

    /**
     * Prints a table displaying specified columns, and checks the expected
     * number of rows.
     */
    private void printTable(String table, String cols,
                            int expected) throws SQLException {

        int               rows = 0;
        ResultSet rs = stmt.executeQuery("SELECT " + cols + " FROM " + table);
        ResultSetMetaData rsmd = rs.getMetaData();
        String result = "Table " + table + ", expecting " + expected
                + " rows total:\n";

        while (rs.next()) {
            for (int i = 0; i < rsmd.getColumnCount(); i++) {
                result += rsmd.getColumnLabel(i + 1) + ":"
                        + rs.getString(i + 1) + ":";
            }

            result += "\n";

            rows++;
        }

        rs.close();
        System.out.println(result);
        assertEquals(expected, rows);
    }

    public void testSimple() throws SQLException{
        stmt =  conn.createStatement();

        String createTable = "CREATE TABLE Emp (Dummy INTEGER, ENo INTEGER, EName VARCHAR(30))";

        String addRow = "INSERT INTO Emp (Dummy, ENo, EName) VALUES (123, 2, 'Seo')";
        String addRow2 = "INSERT INTO Emp (Dummy, ENo, EName) VALUES (456, 2, 'Charlie')";
        String addRow3 = "INSERT INTO Emp (Dummy, ENo, EName) VALUES (456, 3, 'Violet'),(457, 3, 'Violet2'),(458, 3, 'Violet3')";

        stmt.executeUpdate(createTable);
        stmt.executeUpdate(addRow);
        stmt.executeUpdate(addRow2);
        stmt.executeUpdate(addRow3);

        printTable("Emp", "*", 5);
    }

    public void testDeleteForPeriodOf() throws SQLException{
        stmt =  conn.createStatement();

        String createAppTable = "CREATE TABLE Emp (TrigOut VARCHAR(30) NULL, ENo INTEGER, EName VARCHAR(30), EStart DATE, EEnd DATE, PERIOD FOR EPeriod (EStart, EEnd))";

        //T1~T4 should be left, and F1~F4 should not be
        String addRow = "INSERT INTO Emp (TrigOut, ENo, EName, EStart, EEnd) VALUES ('none', 1, 'T1',  '2019-03-01', '2019-03-31')," +
                "('none', 2, 'T2', '2019-03-01', '2019-04-10')," +
                "('none', 3, 'T3', '2019-02-28', '2019-03-31')," +
                "('none', 4, 'T4', '2019-02-28', '2019-04-10')," +
                "('none', 5, 'F1', '2019-03-01', '2019-03-20')," +
                "('none', 6, 'F2', '2019-03-10', '2019-03-31')," +
                "('none', 7, 'F3', '2019-03-10', '2019-03-20')," +
                "('none', 8, 'F4', '2019-05-01', '2019-05-31')";
        String deleteRow = "DELETE FROM Emp FOR PORTION OF EPeriod FROM DATE '2019-03-01' TO DATE '2019-03-31'";

        stmt.executeUpdate(createAppTable);
        stmt.executeUpdate(addRow);
        stmt.executeUpdate(deleteRow);

        printTable("Emp", "*", 4);
    }

    public void testPKInsert() throws SQLException{
        stmt =  conn.createStatement();

        String createAppTable = "CREATE TABLE Emp (ENo INTEGER, EName VARCHAR(30), EStart DATE, EEnd DATE, PERIOD FOR EPeriod (EStart, EEnd), PRIMARY KEY(ENo, EPeriod WITHOUT OVERLAPS))";

        //T1~T4 should be left, and F1~F4 should not be
        String addRow = "INSERT INTO Emp (ENo, EName, EStart, EEnd) VALUES " +
                "(1, 'T1', '2018-03-01', '2019-03-31')," +
                "(2, 'T2', '2019-02-01', '2019-03-01')," +
                "(2, 'T3', '2019-03-01', '2019-04-01')";

        String addRow1 = "INSERT INTO Emp (ENo, EName, EStart, EEnd) VALUES (2, 'F1',  '2019-01-01', '2019-02-01')";
        String addRow2 = "INSERT INTO Emp (ENo, EName, EStart, EEnd) VALUES (2, 'F2',  '2019-04-01', '2019-05-01')";
        String addViolation1 = "INSERT INTO Emp (ENo, EName, EStart, EEnd) VALUES (1, 'F1',  '2018-02-01', '2018-03-02')";
        String addViolation2 = "INSERT INTO Emp (ENo, EName, EStart, EEnd) VALUES (1, 'F2',  '2018-03-30', '2018-04-31')";
        String addViolation3 = "INSERT INTO Emp (ENo, EName, EStart, EEnd) VALUES (1, 'F3',  '2018-03-10', '2018-03-20')";

        stmt.executeUpdate(createAppTable);
        stmt.executeUpdate(addRow);

        stmt.executeUpdate(addRow1);
        stmt.executeUpdate(addRow2);

        int errorCount = 0;
        try{
            stmt.executeUpdate(addViolation1);
        }catch(HsqlException e){
            errorCount++;
        }

        try{
            stmt.executeUpdate(addViolation2);
        }catch(HsqlException e){
            errorCount++;
        }

        try{
            stmt.executeUpdate(addViolation3);
        }catch(HsqlException e){
            errorCount++;
        }

        assertEquals(3, errorCount);
        printTable("Emp", "*", 5);
    }

}
