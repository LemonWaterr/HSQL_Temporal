package org.hsqldb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;


public class TempTest {

    Connection conn;

    public TempTest(){
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
            System.out.println("HSQLDB JDBCDriver Loaded");
            conn = DriverManager.getConnection(
                    "jdbc:hsqldb:Test", "SA", "");
            System.out.println("HSQLDB Connection Created");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() throws SQLException{
        Statement st = conn.createStatement();
        st.execute("SHUTDOWN");
        conn.close();
    }

    public static void dump(ResultSet rs) throws SQLException {

        // the order of the rows in a cursor
        // are implementation dependent unless you use the SQL ORDER statement
        ResultSetMetaData meta   = rs.getMetaData();
        int               colmax = meta.getColumnCount();
        int               i;
        Object            o = null;

        // the result set is a cursor into the data.  You can only
        // point to one row at a time
        // assume we are pointing to BEFORE the first row
        // rs.next() points to next row and returns true
        // or false if there is no next row, which breaks the loop
        for (; rs.next(); ) {
            for (i = 0; i < colmax; ++i) {
                o = rs.getObject(i + 1);    // Is SQL the first column is indexed

                // with 1 not 0
                System.out.print(o.toString() + " ");
            }

            System.out.println(" ");
        }
    }

    public synchronized void sysTest() throws SQLException{
        Statement stmt =  conn.createStatement();

        String init = "DROP TABLE Emp";
        //String createSVTable = "CREATE TABLE Emp (ENo INTEGER, Sys_start TIMESTAMP(12) GENERATED ALWAYS AS ROW START, Sys_end TIMESTAMP(12) GENERATED ALWAYS AS ROW END, EName VARCHAR(30), PERIOD FOR SYSTEM_TIME (Sys_start, Sys_end)) WITH SYSTEM VERSIONING";

        String addRow = "INSERT INTO Emp (ENo, EName) VALUES (2, 'Seo')";
        String updateRow = "UPDATE Emp SET EName = 'Woo' WHERE ENo = 2";

        String createSVTable = "CREATE TABLE Emp (ENo INTEGER, EName VARCHAR(10))";

        stmt.executeUpdate(init);
        stmt.executeUpdate(createSVTable);
        stmt.executeUpdate(addRow);
        stmt.executeUpdate(updateRow);

        stmt.close();
    }


    public synchronized void selectAll() throws SQLException{
        Statement stmt =  conn.createStatement();

        ResultSet rs = stmt.executeQuery("SELECT * FROM Emp");
        dump(rs);

        stmt.close();
    }

    public synchronized void testAppQuery() throws SQLException{
        Statement stmt =  conn.createStatement();

        ResultSet rs = stmt.executeQuery("SELECT * FROM Emp WHERE EPeriod IMMEDIATELY PRECEDES PERIOD (DATE '2019-03-01', DATE '2019-03-31')");
        dump(rs);

        stmt.close();
    }

    public synchronized void testUpdateForPeriodOf() throws SQLException{
        Statement stmt =  conn.createStatement();

        String init = "DROP TABLE Emp";
        String createAppTable = "CREATE TABLE Emp (TrigOut VARCHAR(30) NULL, ENo INTEGER, EName VARCHAR(30), EStart DATE, EEnd DATE, PERIOD FOR EPeriod (EStart, EEnd), PRIMARY KEY(ENo, EPeriod WITHOUT OVERLAPS))";

        //String createTrigger = "CREATE TRIGGER testTrigInsert AFTER INSERT ON Emp FOR EACH ROW BEGIN ATOMIC INSERT INTO Emp VALUES ('trigger', 999, 'Trig',  '2011-01-01', '2011-02-01'); END";

        //T1~T4 should be properly updated, and F1~F4 should not be
        String addRow = "INSERT INTO Emp (TrigOut, ENo, EName, EStart, EEnd) VALUES ('none', 1, 'T1',  '2019-03-01', '2019-03-31')," +
                                                                            "('none', 2, 'T2', '2019-03-01', '2019-04-10')," +
                                                                            "('none', 3, 'T3', '2019-02-28', '2019-03-31')," +
                                                                            "('none', 4, 'T4', '2019-02-28', '2019-04-10')," +
                                                                            "('none', 5, 'F1', '2019-03-01', '2019-03-20')," +
                                                                            "('none', 6, 'F2', '2019-03-10', '2019-03-31')," +
                                                                            "('none', 7, 'F3', '2019-03-10', '2019-03-20')," +
                                                                            "('none', 8, 'F4', '2019-05-01', '2019-05-31')";
        String updateRow = "UPDATE Emp FOR PORTION OF EPeriod FROM DATE '2019-03-01' TO DATE '2019-03-31' SET EName = 'Changed'";

        stmt.executeUpdate(init);
        stmt.executeUpdate(createAppTable);
        //stmt.executeUpdate(createTrigger);
        stmt.executeUpdate(addRow);
        stmt.executeUpdate(updateRow);

        stmt.close();
    }

    public synchronized void testDeleteForPeriodOf() throws SQLException{
        Statement stmt =  conn.createStatement();

        String init = "DROP TABLE Emp";
        String createAppTable = "CREATE TABLE Emp (TrigOut VARCHAR(30) NULL, ENo INTEGER, EName VARCHAR(30), EStart DATE, EEnd DATE, PERIOD FOR EPeriod (EStart, EEnd))";

        //String createTrigger = "CREATE TRIGGER testTrigInsert AFTER INSERT ON Emp FOR EACH ROW BEGIN ATOMIC INSERT INTO Emp VALUES ('trigger', 999, 'Trig',  '2011-01-01', '2011-02-01'); END";

        //T1~T4 should be properly updated, and F1~F4 should not be
        String addRow = "INSERT INTO Emp (TrigOut, ENo, EName, EStart, EEnd) VALUES ('none', 1, 'T1',  '2019-03-01', '2019-03-31')," +
                "('none', 2, 'T2', '2019-03-01', '2019-04-10')," +
                "('none', 3, 'T3', '2019-02-28', '2019-03-31')," +
                "('none', 4, 'T4', '2019-02-28', '2019-04-10')," +
                "('none', 5, 'F1', '2019-03-01', '2019-03-20')," +
                "('none', 6, 'F2', '2019-03-10', '2019-03-31')," +
                "('none', 7, 'F3', '2019-03-10', '2019-03-20')," +
                "('none', 8, 'F4', '2019-05-01', '2019-05-31')";
        String deleteRow = "DELETE FROM Emp FOR PORTION OF EPeriod FROM DATE '2019-03-01' TO DATE '2019-03-31'";

        stmt.executeUpdate(init);
        stmt.executeUpdate(createAppTable);
        //stmt.executeUpdate(createTrigger);
        stmt.executeUpdate(addRow);
        stmt.executeUpdate(deleteRow);

        stmt.close();
    }

    public synchronized void testPK() throws SQLException{
        Statement stmt =  conn.createStatement();

        String init = "DROP TABLE Emp";
        String createAppTable = "CREATE TABLE Emp (Dummy VARCHAR(30) NULL, ENo INTEGER, EName VARCHAR(30), EStart DATE, EEnd DATE, PERIOD FOR EPeriod (EStart, EEnd), PRIMARY KEY(ENo, EPERIOD WITHOUT OVERLAPS))";

        //String createTrigger = "CREATE TRIGGER testTrigInsert AFTER INSERT ON Emp FOR EACH ROW BEGIN ATOMIC INSERT INTO Emp VALUES ('trigger', 999, 'Trig',  '2011-01-01', '2011-02-01'); END";

        //T1~T4 should be properly updated, and F1~F4 should not be
        String addRow = "INSERT INTO Emp (Dummy, ENo, EName, EStart, EEnd) VALUES ('asdf', 1, 'T1',  '2019-02-01', '2019-02-28')," +
                "('none', 2, 'T2', '2019-03-01', '2019-04-10')," +
                "('asdf', 3, 'T3', '2019-02-01', '2019-02-15')," +
                "('none', 4, 'T4', '2019-02-28', '2019-04-10')," +
                "('none', 5, 'F1', '2019-03-01', '2019-03-20')," +
                "('none', 6, 'F2', '2019-03-10', '2019-03-31')," +
                "('none', 7, 'F3', '2019-03-10', '2019-03-20')," +
                "('none', 8, 'F4', '2019-05-01', '2019-05-31')";

        String addViolation = "INSERT INTO Emp (Dummy, ENo, EName, EStart, EEnd) VALUES ('none', 1, 'T11',  '2019-03-01', '2019-03-31')";
        String updateRow = "UPDATE Emp SET ENo=2 WHERE Dummy='asdf'";

        stmt.executeUpdate(init);
        stmt.executeUpdate(createAppTable);
        //stmt.executeUpdate(createTrigger);
        stmt.executeUpdate(addRow);
        //stmt.executeUpdate(addViolation);
        stmt.executeUpdate(updateRow);

        stmt.close();
    }

    public synchronized void testDMLConstraint() throws SQLException{
        Statement stmt =  conn.createStatement();

        String init = "DROP TABLE Emp";
        String createAppTable = "CREATE TABLE Emp (TrigOut VARCHAR(30) NULL, ENo INTEGER, EName VARCHAR(30), EStart DATE, EEnd DATE, PERIOD FOR EPeriod (EStart, EEnd), PRIMARY KEY(ENo, EPeriod WITHOUT OVERLAPS))";

        //String createTrigger = "CREATE TRIGGER testTrigInsert AFTER INSERT ON Emp FOR EACH ROW BEGIN ATOMIC INSERT INTO Emp VALUES ('trigger', 999, 'Trig',  '2011-01-01', '2011-02-01'); END";

        //T1~T4 should be properly updated, and F1~F4 should not be
        String addRow = "INSERT INTO Emp (TrigOut, ENo, EName, EStart, EEnd) VALUES ('none', 1, 'A',  '2019-03-01', '2019-03-31')";
        String addRow2 = "INSERT INTO Emp (TrigOut, ENo, EName, EStart, EEnd) VALUES ('none', 1, 'B',  '2019-04-01', '2019-04-30')";

        String deleteRow = "DELETE FROM Emp FOR PORTION OF EPeriod FROM DATE '2019-03-01' TO DATE '2019-03-15'";

        stmt.executeUpdate(init);
        stmt.executeUpdate(createAppTable);
        //stmt.executeUpdate(createTrigger);
        stmt.executeUpdate(addRow);
        stmt.executeUpdate(addRow2);

        stmt.close();
    }

    public synchronized void testSimple() throws SQLException{
        Statement stmt =  conn.createStatement();

        String init = "DROP TABLE Emp";
        String createTable = "CREATE TABLE Emp (Dummy INTEGER, ENo INTEGER, EName VARCHAR(30))";

        String addRow = "INSERT INTO Emp (Dummy, ENo, EName) VALUES (123, 2, 'Seo')";
        String addRow2 = "INSERT INTO Emp (Dummy, ENo, EName) VALUES (456, 2, 'Charlie')";
        String addRow3 = "INSERT INTO Emp (Dummy, ENo, EName) VALUES (456, 3, 'Violet'),(457, 3, 'Violet2'),(458, 3, 'Violet3')";
        String updateRow = "UPDATE Emp SET EName = 'Woo', Dummy = 999 WHERE ENo < 3 AND EName = 'Seo'";

        stmt.executeUpdate(init);
        stmt.executeUpdate(createTable);
        stmt.executeUpdate(addRow);
        stmt.executeUpdate(addRow2);
        stmt.executeUpdate(addRow3);
        stmt.executeUpdate(updateRow);

        stmt.close();
    }


    public synchronized void TestConstraint() throws SQLException{
        Statement stmt =  conn.createStatement();

        String init = "DROP TABLE Emp";
        String createTable = "CREATE TABLE Emp (Num1 INTEGER, Num2 INTEGER, EName VARCHAR(30), CONSTRAINT CHK CHECK (Num1 < Num2) )";

        String dropcon = "ALTER TABLE Emp DROP CONSTRAINT CHK";

        //String addRow = "INSERT INTO Emp (Num1, Num2, EName) VALUES (3, 4, 'Valid')";
        //String addRow2 = "INSERT INTO Emp (Num1, Num2, EName) VALUES (90, 10, 'Invalid')";
        //String updateRow = "UPDATE Emp SET EName = 'Invalid', Num1 = 5 WHERE Num1 == 3";

        stmt.executeUpdate(init);
        stmt.executeUpdate(createTable);
        stmt.executeUpdate(dropcon);
        //stmt.executeUpdate(addRow);
        //stmt.executeUpdate(addRow2);
        //stmt.executeUpdate(updateRow);

        stmt.close();
    }

    public synchronized void TestConstraint2() throws SQLException{
        Statement stmt =  conn.createStatement();

        String init = "DROP TABLE Emp";
        String createAppTable = "CREATE TABLE Emp (TrigOut VARCHAR(30) NULL, ENo INTEGER, EName VARCHAR(30), EStart DATE, EEnd DATE, PERIOD FOR EPeriod (EStart, EEnd))";

        //T1~T4 should be properly updated, and F1~F4 should not be
        //String addRow = "INSERT INTO Emp (TrigOut, ENo, EName, EStart, EEnd) VALUES ('none', 1, 'T1', '2019-03-01', '2019-03-31')";
        String addRow = "INSERT INTO Emp (TrigOut, ENo, EName, EStart, EEnd) VALUES ('none', 1, 'T1', '2019-03-31', '2019-03-01')";
        String updateRow = "UPDATE Emp SET EStart = '2019-04-01'";

        stmt.executeUpdate(init);
        stmt.executeUpdate(createAppTable);
        //stmt.executeUpdate(createTrigger);
        stmt.executeUpdate(addRow);
        //stmt.executeUpdate(updateRow);

        stmt.close();
    }


    public static void main(String[] args) {
        TempTest test = new TempTest();
        try {
            test.testPK();
            //test.testUpdateForPeriodOf();
            System.out.println("------------------------");
            test.selectAll();
            //test.TestAppQuery();
            test.shutdown();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
    }
}
