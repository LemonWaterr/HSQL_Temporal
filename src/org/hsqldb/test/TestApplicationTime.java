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

            stmt =  conn.createStatement();

            String createAppTable = "CREATE TABLE Emp (Trig VARCHAR(30) NULL, ENo INTEGER, EName VARCHAR(30), EStart DATE, EEnd DATE, PERIOD FOR EPeriod (EStart, EEnd), PRIMARY KEY(ENo, EPeriod WITHOUT OVERLAPS))";
            String addRow = "INSERT INTO Emp (ENo, EName, EStart, EEnd) VALUES " +
                    "(1, 'T1', '2019-03-01', '2019-03-31')," +
                    "(2, 'T2', '2019-03-01', '2019-04-10')," +
                    "(3, 'T3', '2019-02-28', '2019-03-31')," +
                    "(4, 'T4', '2019-02-28', '2019-04-10')," +
                    "(5, 'F1', '2019-03-01', '2019-03-20')," +
                    "(6, 'F2', '2019-03-10', '2019-03-31')," +
                    "(7, 'F3', '2019-03-10', '2019-03-20')," +
                    "(8, 'F4', '2019-05-01', '2019-05-31')";

            stmt.executeUpdate(createAppTable);
            stmt.executeUpdate(addRow);

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void tearDown() {

        try {
            stmt.execute("DROP TABLE Emp IF EXISTS CASCADE");
            stmt.execute("DROP TABLE Dept IF EXISTS CASCADE");
            stmt.execute("DROP TABLE Trig IF EXISTS CASCADE");
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
    private void printTable(String table, String cols, String where,
                            int expected) throws SQLException {

        int               rows = 0;
        ResultSet rs = stmt.executeQuery("SELECT " + cols + " FROM " + table + " " + where);
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

    public void testUpdateForPortionOf() throws SQLException{

        String createTrigTable = "CREATE TABLE Trig (ENo INTEGER, Type VARCHAR(30))";

        String createTrigger1 = "CREATE TRIGGER testTrigAfterInsert AFTER INSERT ON Emp REFERENCING NEW ROW AS newrow FOR EACH ROW INSERT INTO Trig(ENo, Type) VALUES (newrow.ENo, 'AFTER_INSERT')";
        String createTrigger2 = "CREATE TRIGGER testTrigBeforeInsert BEFORE INSERT ON Emp REFERENCING NEW ROW AS newrow FOR EACH ROW SET newrow.Trig = 'BEFORE_INSERT'";
        String createTrigger3 = "CREATE TRIGGER testTrigAfterUpdate AFTER UPDATE ON Emp REFERENCING NEW ROW AS newrow FOR EACH ROW INSERT INTO Trig(ENo, Type) VALUES (newrow.ENo, 'AFTER_UPDATE')";
        String createTrigger4 = "CREATE TRIGGER testTrigBeforeUpdate BEFORE UPDATE ON Emp REFERENCING NEW ROW AS newrow FOR EACH ROW SET newrow.Trig = 'BEFORE_UPDATE'";

        String updateRow = "UPDATE Emp FOR PORTION OF EPeriod FROM DATE '2019-03-01' TO DATE '2019-03-31' SET EName = 'Changed'";


        stmt.executeUpdate(createTrigTable);
        stmt.executeUpdate(createTrigger1);
        stmt.executeUpdate(createTrigger2);
        stmt.executeUpdate(createTrigger3);
        stmt.executeUpdate(createTrigger4);
        stmt.executeUpdate(updateRow);

        printTable("Emp",   "*","WHERE Trig = 'BEFORE_UPDATE' AND EName = 'Changed'", 4); //check update
        printTable("Emp",   "*","WHERE Trig = 'BEFORE_INSERT'", 4); //check extra row insert
        printTable("Trig",  "*","WHERE Type = 'AFTER_INSERT'", 4); //check extra row fired after insert trigger
        printTable("Trig",  "*","WHERE Type = 'AFTER_UPDATE'", 4); //check after update trigger fired
    }

    public void testDeleteForPortionOf() throws SQLException{

        String createTrigTable = "CREATE TABLE Trig (ENo INTEGER, Type VARCHAR(30))";

        String createTrigger1 = "CREATE TRIGGER testTrigAfterInsert AFTER INSERT ON Emp REFERENCING NEW ROW AS newrow FOR EACH ROW INSERT INTO Trig(ENo, Type) VALUES (newrow.ENo, 'AFTER_INSERT')";
        String createTrigger2 = "CREATE TRIGGER testTrigBeforeInsert BEFORE INSERT ON Emp REFERENCING NEW ROW AS newrow FOR EACH ROW SET newrow.Trig = 'BEFORE_INSERT'";
        String createTrigger3 = "CREATE TRIGGER testTrigAfterUpdate AFTER DELETE ON Emp REFERENCING OLD ROW AS oldrow FOR EACH ROW INSERT INTO Trig(ENo, Type) VALUES (oldrow.ENo, 'AFTER_DELETE')";

        String deleteRow = "DELETE FROM Emp FOR PORTION OF EPeriod FROM DATE '2019-03-01' TO DATE '2019-03-31'";

        stmt.executeUpdate(createTrigTable);
        stmt.executeUpdate(createTrigger1);
        stmt.executeUpdate(createTrigger2);
        stmt.executeUpdate(createTrigger3);
        stmt.executeUpdate(deleteRow);

        printTable("Emp",   "*","", 8); //check delete
        printTable("Emp",   "*","WHERE Trig = 'BEFORE_INSERT'", 4); //check extra row insert
        printTable("Trig",  "*","WHERE Type = 'AFTER_INSERT'", 4); //check extra rows + their trigger fired
        printTable("Trig",  "*","WHERE Type = 'AFTER_DELETE'", 4); //check update trigger fired
    }

    public void testPKInsert() throws SQLException{
        stmt =  conn.createStatement();

        /*
        "('init', 1, 'T1', '2019-03-01', '2019-03-31')," +
        "('init', 2, 'T2', '2019-03-01', '2019-04-10')," +
        "('init', 3, 'T3', '2019-02-28', '2019-03-31')," +
        "('init', 4, 'T4', '2019-02-28', '2019-04-10')," +
        "('init', 5, 'F1', '2019-03-01', '2019-03-20')," +
        "('init', 6, 'F2', '2019-03-10', '2019-03-31')," +
        "('init', 7, 'F3', '2019-03-10', '2019-03-20')," +
        "('init', 8, 'F4', '2019-05-01', '2019-05-31')";
        */

        String addRow1 = "INSERT INTO Emp (ENo, EName, EStart, EEnd) VALUES (1, 'T11',  '2019-02-01', '2019-03-01')";
        String addRow2 = "INSERT INTO Emp (ENo, EName, EStart, EEnd) VALUES (1, 'T11',  '2019-03-31', '2019-04-01')";

        stmt.executeUpdate(addRow1);
        stmt.executeUpdate(addRow2);

        printTable("Emp", "*", "",10);

        String addViolation = "INSERT INTO Emp (ENo, EName, EStart, EEnd) VALUES (1, 'F1',  '2018-02-01', '2018-03-02')";

        try{
            stmt.executeUpdate(addViolation);
            fail("Missing exception");
        }catch(HsqlException e){
            assertTrue(true);
        }
    }

}
