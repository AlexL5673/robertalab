package de.fhg.iais.roberta.persistence.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.iais.roberta.util.dbc.Assert;
import de.fhg.iais.roberta.util.dbc.DbcException;

public class DbExecutor {
    private static Logger LOG = LoggerFactory.getLogger(DbExecutor.class);

    private final Session session;

    public static DbExecutor make(Session session) {
        return new DbExecutor(session);
    }

    private DbExecutor(Session session) {
        this.session = session;
    }

    /**
     * execute the sql statements found in a resource. Check if the execution is necessary. It not, return. After loading, check if the execution was
     * successful. If not, throw an exception.
     *
     * @param sqlResource contains SQL statements separated by a semicolon AT THE END of a LINE.
     * @param sqlPositiveIfSqlFileAlreadyLoaded sql returning positive number if sql file has already been loaded. May be null. Executed before loading.
     * @param sqlPositiveIfSetupSuccessful sql returning positive number if sql file has been successfully loaded. May be null. executed after loading.
     * @throws Exception
     */
    public void sqlFile(String sqlResource, String sqlPositiveIfSqlFileAlreadyLoaded, String sqlPositiveIfSetupSuccessful) throws Exception {
        int result = 0;
        if ( sqlPositiveIfSqlFileAlreadyLoaded != null ) {
            result = ((BigInteger) oneValueSelect(sqlPositiveIfSqlFileAlreadyLoaded)).intValue();
        }
        if ( result == 0 ) {
            sqlFile(this.getClass().getResourceAsStream(sqlResource));
            if ( sqlPositiveIfSetupSuccessful != null ) {
                result = ((BigInteger) oneValueSelect(sqlPositiveIfSetupSuccessful)).intValue();
                if ( result <= 0 ) {
                    throw new DbcException("loading sql from resource " + sqlResource + " was NOT successful");
                }
            }
        } else {
            DbExecutor.LOG.info("test sql says, that DDL of  " + sqlResource + " has already been executed. Skipping execution.");
        }
    }

    public void sqlFile(InputStream sqlStmtFileStream) {
        String line = "";
        int count = 0;
        try {
            Reader reader = new InputStreamReader(sqlStmtFileStream, "UTF-8");
            BufferedReader in = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            while ( (line = in.readLine()) != null ) {
                line = line.trim();
                if ( line.startsWith("--") || line.equals("") ) {
                    // next
                } else if ( line.endsWith(";") ) {
                    line = line.substring(0, line.length() - 1);
                    sb.append(line);
                    String sqlStmt = sb.toString().trim();
                    if ( sqlStmt.equals("") ) {
                        // leeres stmt
                    } else {
                        count++;
                        sqlStmt(sqlStmt);
                    }
                    this.session.flush();
                    sb = new StringBuilder();
                } else {
                    sb.append(line);
                    sb.append(" \n");
                }
            }
            sqlStmtFileStream.close();
            DbExecutor.LOG.info(count + " SQL-statements executed");
        } catch ( Exception e ) {
            DbExecutor.LOG.error("Exception in sql stmt: " + count, e);
        } finally {
            if ( sqlStmtFileStream != null ) {
                try {
                    sqlStmtFileStream.close();
                } catch ( Throwable t ) {
                    // ok here
                }
            }
        }
    }

    public void sqlStmt(String sqlStmt) {
        if ( DbExecutor.isSelect(sqlStmt) ) {
            DbExecutor.LOG.debug("SQL: " + sqlStmt);
            select(sqlStmt);
        } else if ( DbExecutor.isChange(sqlStmt) ) {
            DbExecutor.LOG.debug("UPD: " + sqlStmt);
            update(sqlStmt);
        } else if ( DbExecutor.isDDL(sqlStmt) ) {
            DbExecutor.LOG.debug("DDL: " + sqlStmt);
            ddl(sqlStmt);
        } else {
            DbExecutor.LOG.error("Ignored: " + sqlStmt);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> select(String sqlStmt) {
        List<Object[]> resultSet = this.session.createSQLQuery(sqlStmt).list();
        DbExecutor.LOG.debug("got " + resultSet.size() + " rows");
        for ( Object result : resultSet ) {
            if ( result instanceof Object[] ) {
                DbExecutor.LOG.debug("  " + Arrays.toString((Object[]) result));
            } else {
                DbExecutor.LOG.debug("  " + result);
            }
        }
        return (List<T>) resultSet;
    }

    public Object oneValueSelect(String sqlStmt) {
        @SuppressWarnings("unchecked")
        List<Object> resultSet = this.session.createSQLQuery(sqlStmt).list();
        Assert.isTrue(resultSet.size() == 1, "result set should contain 1 row, but contains " + resultSet.size() + " rows");
        Object result = resultSet.get(0);
        DbExecutor.LOG.debug(result == null ? "null" : result.toString());
        return result;
    }

    public int update(String sqlStmt) {
        int result = this.session.createSQLQuery(sqlStmt).executeUpdate();
        DbExecutor.LOG.debug("rows affected: " + result);
        return result;
    }

    public int ddl(String sqlStmt) {
        int result = this.session.createSQLQuery(sqlStmt).executeUpdate();
        DbExecutor.LOG.debug("rows affected (probably 0): " + result);
        return result;
    }

    public static boolean isSelect(String sqlStmt) {
        return DbExecutor.sW(sqlStmt, "select ");
    }

    public static boolean isChange(String sqlStmt) {
        return DbExecutor.sW(sqlStmt, "insert ") || DbExecutor.sW(sqlStmt, "update ") || DbExecutor.sW(sqlStmt, "delete ") || sqlStmt.trim().equals("commit");
    }

    public static boolean isDDL(String sqlStmt) {
        return DbExecutor.sW(sqlStmt, "drop ") || DbExecutor.sW(sqlStmt, "create ") || DbExecutor.sW(sqlStmt, "alter ") || DbExecutor.sW(sqlStmt, "backup ");
    }

    private static boolean sW(String testString, String expected) {
        if ( testString.length() < expected.length() ) {
            return false;
        }
        return testString.substring(0, expected.length()).equalsIgnoreCase(expected);
    }
}