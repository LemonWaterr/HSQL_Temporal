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

        System.out.println("testUpdateForPortionOf");
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

        System.out.println("testDeleteForPortionOf");
        printTable("Emp",   "*","", 8); //check delete
        printTable("Emp",   "*","WHERE Trig = 'BEFORE_INSERT'", 4); //check extra row insert
        printTable("Trig",  "*","WHERE Type = 'AFTER_INSERT'", 4); //check extra rows + their trigger fired
        printTable("Trig",  "*","WHERE Type = 'AFTER_DELETE'", 4); //check update trigger fired
    }

    public void testPKInsert() throws SQLException{
        stmt =  conn.createStatement();

        //non-violating rows inserted
        String addRow1 = "INSERT INTO Emp (ENo, EName, EStart, EEnd) VALUES (1, 'T11',  '2019-02-01', '2019-03-01')";
        String addRow2 = "INSERT INTO Emp (ENo, EName, EStart, EEnd) VALUES (1, 'T12',  '2019-03-31', '2019-04-01')";

        stmt.executeUpdate(addRow1);
        stmt.executeUpdate(addRow2);

        System.out.println("testPKInsert");
        printTable("Emp", "*", "",10);

        String addViolation = "INSERT INTO Emp (ENo, EName, EStart, EEnd) VALUES (1, 'F',  '2019-02-01', '2019-03-02')";

        try{
            stmt.executeUpdate(addViolation);
            fail("Missing exception");
        }catch(HsqlException e){
            assertTrue(true);
        }
    }

    public void testPKInsert2() throws SQLException{
        stmt =  conn.createStatement();

        String addViolation = "INSERT INTO Emp (ENo, EName, EStart, EEnd) VALUES (9, 'F91',  '2019-02-01', '2019-03-01'), (9, 'F92',  '2019-02-28', '2019-04-01')";

        try{
            stmt.executeUpdate(addViolation);
            fail("Missing exception");
        }catch(HsqlException e){
            assertTrue(true);
        }
    }

    public void testPKUpdate() throws SQLException{
        stmt =  conn.createStatement();

        //use different table content
        stmt.execute("DELETE FROM Emp");

        String addRow = "INSERT INTO Emp (ENo, EName, EStart, EEnd) VALUES " +
                "(1, 'T1',  '2019-02-01', '2019-02-28')," +
                "(2, 'T2', '2019-03-01', '2019-04-10')," +
                "(3, 'T3', '2019-02-01', '2019-02-15')";
        stmt.executeUpdate(addRow);

        String updateRow = "UPDATE Emp SET ENo = 2 WHERE EName = 'T1'";
        stmt.executeUpdate(updateRow); //this should work fine

        String updateViolation = "UPDATE Emp SET ENo = 2 WHERE EName = 'T3'";

        try{
            stmt.executeUpdate(updateViolation);
            fail("Missing exception");
        }catch(HsqlException e){
            assertTrue(true);
        }
    }

    public void testPKUpdate2() throws SQLException{
        stmt =  conn.createStatement();

        //use different table content
        stmt.execute("DELETE FROM Emp");

        String addRow = "INSERT INTO Emp (ENo, EName, EStart, EEnd) VALUES " +
                "(1, 'T1',  '2019-02-01', '2019-02-28')," +
                "(2, 'T2', '2019-03-01', '2019-04-10')," +
                "(3, 'T3', '2019-02-01', '2019-02-15')";
        stmt.executeUpdate(addRow);

        String updateViolation = "UPDATE Emp SET ENo = 2 WHERE EName = 'T1' OR EName = 'T3'";

        try{
            stmt.executeUpdate(updateViolation);
            fail("Missing exception");
        }catch(HsqlException e){
            assertTrue(true);
        }
    }

    public void testFKInsert() throws SQLException{

        //use different table
        stmt.execute("DROP TABLE Emp IF EXISTS CASCADE");

        String createParent = "CREATE TABLE Dept (DNo INTEGER, DName VARCHAR(30), DStart DATE, DEnd DATE, Period FOR DPeriod (DStart, DEnd), PRIMARY KEY(DNo, DPERIOD))";
        String createChild = "CREATE TABLE Emp (ENo INTEGER, EName VARCHAR(30), EDept INTEGER, EStart DATE, EEnd DATE, PERIOD FOR EPeriod (EStart, EEnd), CONSTRAINT FK_test FOREIGN KEY (EDept, PERIOD EPeriod) REFERENCES Dept (DNo, PERIOD DPeriod) ON DELETE CASCADE ON UPDATE SET NULL)";

        String addParentRow = "INSERT INTO Dept (DNo, DName, DStart, DEnd) VALUES " +
                "(3, 'Test1', '2007-01-01', '2007-12-31')," +
                "(3, 'Test2', '2009-01-01', '2009-12-31')," +
                "(3, 'Test3', '2011-01-01', '2011-12-31')," +
                "(3, 'Test4', '2012-01-01', '2012-12-30')," +
                "(4, 'QA',   '2011-06-01', '2011-12-31')";

        String addChildRow = "INSERT INTO Emp (ENo, EName, EDept, EStart, EEnd) VALUES " +
                "(22218,  'T1', 3, '2007-01-01', '2007-12-31')," +
                "(21119,  'T2', 3, '2009-01-01', '2009-05-31')," +
                "(21119,  'T3', 3, '2009-05-01', '2009-12-31')," +
                "(30000, 'Seo', 4, '2011-05-01', '2011-06-01')"; //The violating Row

        stmt.executeUpdate(createParent);
        stmt.executeUpdate(createChild);
        stmt.executeUpdate(addParentRow);
        try{
            stmt.executeUpdate(addChildRow);
            fail("Missing exception");
        }catch(HsqlException e){
            assertTrue(true);
        }
    }

    public void testFK() throws SQLException{

        //use different table
        stmt.execute("DROP TABLE Emp IF EXISTS CASCADE");

        String createParent = "CREATE TABLE Dept (DNo INTEGER, DName VARCHAR(30), DStart DATE, DEnd DATE, Period FOR DPeriod (DStart, DEnd), PRIMARY KEY(DNo, DPERIOD))";
        String createChild = "CREATE TABLE Emp (ENo INTEGER, EName VARCHAR(30), EDept INTEGER, EStart DATE, EEnd DATE, PERIOD FOR EPeriod (EStart, EEnd), CONSTRAINT FK_test FOREIGN KEY (EDept, PERIOD EPeriod) REFERENCES Dept (DNo, PERIOD DPeriod) ON DELETE CASCADE ON UPDATE SET NULL)";

        String addParentRow = "INSERT INTO Dept (DNo, DName, DStart, DEnd) VALUES " +
                "(3, 'Test1', '2007-01-01', '2007-12-31')," +
                "(3, 'Test2', '2009-01-01', '2009-12-31')," +
                "(3, 'Test3', '2011-01-01', '2011-12-31')," +
                "(3, 'Test4', '2012-01-01', '2012-12-30')," +
                "(4, 'QA',   '2011-06-01', '2011-12-31')";

        String addChildRow = "INSERT INTO Emp (ENo, EName, EDept, EStart, EEnd) VALUES " +
                "(22218,  'T1', 3, '2007-01-01', '2007-12-31')," +
                "(21119,  'T2', 3, '2009-01-01', '2009-05-31')," +
                "(21119,  'T3', 3, '2009-05-01', '2009-12-31')," +
                "(21119,  'T4', 3, '2011-05-01', '2011-12-31')," +
                "(30000, 'Seo', 4, '2011-07-01', '2011-12-31')";

        String deleteRow = "DELETE FROM Dept WHERE DName = 'Test2'";

        stmt.executeUpdate(createParent);
        stmt.executeUpdate(createChild);
        stmt.executeUpdate(addParentRow);
        stmt.executeUpdate(addChildRow);
        stmt.executeUpdate(deleteRow);

        System.out.println("testFKDeleteCascade");
        printTable("Emp",   "*","", 3);

        String updateRow = "UPDATE Dept SET DNo=5 WHERE DName = 'Test1'";
        stmt.executeUpdate(updateRow);

        System.out.println("testFKUpdateSetNull");
        printTable("Emp",   "*","WHERE EDept IS NULL AND EStart IS NULL AND EEnd IS NULL", 1);
    }

}
