package org.hsqldb.test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import junit.framework.TestCase;
import junit.framework.TestResult;

public class TestTemporal extends TestBase {

    Statement         stmt;
    PreparedStatement pstmnt;
    Connection        connection;
    String            getColumnName = "false";

    public TestTemporal(String name) {
        super(name);
    }

    protected void setUp() throws Exception {

        super.setUp();

        connection = super.newConnection();
        stmt      = connection.createStatement();
    }

    public void testTemp(){

        String createSVTable = "CREATE TABLE Emp (ENo INTEGER, Sys_start TIMESTAMP(12) GENERATED ALWAYS AS ROW START, Sys_end TIMESTAMP(12) GENERATED ALWAYS AS ROW END, EName VARCHAR(30), PERIOD FOR SYSTEM_TIME (Sys_start, Sys_end)) WITH SYSTEM VERSIONING";
        String addRow = "INSERT INTO Emp (ENo, EName) VALUES (1, 'Seo')";

        System.out.println("please tell me I can see print statements1");

        try {
            stmt.execute(createSVTable);
            stmt.execute(addRow);

            ResultSet rs = stmt.executeQuery("SELECT * FROM Emp");
            System.out.println(rs);
        }catch (SQLException e) {
            fail(e.getMessage());
        }


        assertEquals(1, 1);
    }

}
