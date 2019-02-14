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

    public synchronized void doTest() throws SQLException{
        Statement stmt =  conn.createStatement();

        String createSVTable = "CREATE TABLE Emp (ENo INTEGER, Sys_start TIMESTAMP(12) GENERATED ALWAYS AS ROW START, Sys_end TIMESTAMP(12) GENERATED ALWAYS AS ROW END, EName VARCHAR(30), PERIOD FOR SYSTEM_TIME (Sys_start, Sys_end)) WITH SYSTEM VERSIONING";
        String addRow = "INSERT INTO Emp (ENo, EName) VALUES (2, 'Seo')";

        //String createSVTable = "CREATE TABLE Emp (ENo INTEGER, EName VARCHAR(10))";

        stmt.executeUpdate(createSVTable);
        stmt.executeUpdate(addRow);

        stmt.close();
    }

    public synchronized void doTest2() throws SQLException{
        Statement stmt =  conn.createStatement();

        ResultSet rs = stmt.executeQuery("SELECT * FROM Emp");
        dump(rs);

        stmt.close();
    }

    public static void main(String[] args) {
        TempTest test = new TempTest();
        try {
            test.doTest();
            test.doTest2();
            test.shutdown();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
    }
}
