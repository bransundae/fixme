package com.core.database;

import com.core.MarketSnapshot;
import com.core.Message;
import com.core.Order;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.sql.*;

public class Database implements Callable {

    private String action = "CREATE";

    private static final String DB_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final String JDBC_URL = "jdbc:derby:fixmedb;create=true";
    private static final String SQL_SELECT = "select * from transactions";
    private static final String SQL_INSERT = "insert into transactions (id, message, response) values";
    private static final String SQL_DELETE = "delete from transactions";
    private static final String SQL_CREATE = "create table transactions(id varchar(255), message varchar(255), response boolean)";
    private static final String SQL_DROP = "drop table transactions";
    private static final String SQL_UPDATE = "update transactions set";
    private Statement statement;
    private Connection connection = null;
    private Object message;
    private int rowCount;

    private boolean busy = false;

    public Database() throws ClassNotFoundException{
        Class.forName(DB_DRIVER);
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setMessage(Object message) {
        this.message = message;
    }

    private boolean existDB() throws SQLException {
        createConnection();
        ResultSet res = connection.getMetaData().getTables(null, "APP", "TRANSACTIONS", null);
        if (res.next()) {
            closeConnection();
            return true;
        } else {
            closeConnection();
            return false;
        }
    }

    private void createDB() throws SQLException {
        if (!busy) {
            busy = true;
            createConnection();
            if (existDB()) {
                System.out.println("Database Already Exists");
            } else {
                if(connection == null)
                    createConnection();
                connection.createStatement().execute(SQL_CREATE);
                System.out.println("Database Successfully Created");
                closeConnection();
            }
            busy = false;
        }
    }

    private void dropDB() throws SQLException {
        if (!busy) {
            busy = true;
            if (existDB()) {
                createConnection();
                connection.createStatement().execute(SQL_DROP);
                System.out.println("Database Successfully Dropped");
                closeConnection();
            } else {
                System.out.println("Database Does Not Exist");
            }
            busy = false;
        }
    }

    private void insertTransaction(Object object) throws SQLException {
        long id = -1;
        if (!busy) {
            busy = true;
            createConnection();
            if (((Message)object).getType().equalsIgnoreCase("D")
                    || ((Message)object).getType().equalsIgnoreCase("8")
                    || ((Message)object).getType().equalsIgnoreCase("j")){
                PreparedStatement preparedStatement = connection.prepareStatement(SQL_INSERT
                        + " ('" + ((Order)object).getId()
                        + "','" + ((Order)object).toFix()
                        + "'," + false + ")");
                preparedStatement.execute();
            } else if (((Message)object).getType().equalsIgnoreCase("W")
                    || ((Message)object).getType().equalsIgnoreCase("Y")
                    || ((Message)object).getType().equalsIgnoreCase("V")){
                PreparedStatement preparedStatement = connection.prepareStatement(SQL_INSERT
                        + " ('" + ((MarketSnapshot)message).getId()
                        + "','" + ((MarketSnapshot)message).toFix()
                        + "'," + false + ")");
                preparedStatement.execute();
            } else {
                PreparedStatement preparedStatement = connection.prepareStatement(SQL_INSERT
                        + " ('" + ((Message)object).getId()
                        + "','" + ((Message)object).toFix()
                        + "'," + false + ")");
                preparedStatement.execute();
            }
            closeConnection();
            busy = false;
        }
    }

    private void setTransactionDone(Object object) throws SQLException {
        if(!busy) {
            busy = true;
            createConnection();
            if (((Message)object).getType().equalsIgnoreCase("D")
                    || ((Message)object).getType().equalsIgnoreCase("8")
                    || ((Message)object).getType().equalsIgnoreCase("j")){
                connection.createStatement().execute(SQL_UPDATE + " response = " + true + " where id = " + ((Order)object).getId());
            } else if (((Message)object).getType().equalsIgnoreCase("W")
                    || ((Message)object).getType().equalsIgnoreCase("Y")
                    || ((Message)object).getType().equalsIgnoreCase("V")){
                connection.createStatement().execute(SQL_UPDATE + " response = " + true + " where id = " + ((MarketSnapshot)message).getId());
            } else {
                connection.createStatement().execute(SQL_UPDATE + " response = " + true + " where id = " + ((Message)message).getId());
            }
            closeConnection();
            busy = false;
        }
    }

    private Message queryMessage(String id) throws SQLException {
        ResultSet resultSet = null;
        Message message = null;
        if (!busy) {
            busy = true;
            createConnection();
            resultSet = statement.executeQuery(SQL_SELECT + " where id = " + id);
            busy = false;
        }
        if (resultSet != null) {
            if (resultSet.next()) {
                message = new Message(resultSet.getString(2));
            }
        }
        return message;
    }

    private ArrayList<Message> queryMessages() throws SQLException {
        ResultSet resultSet = null;
        ArrayList<Message> messages = null;
        if (!busy) {
            busy = true;
            createConnection();
            resultSet = statement.executeQuery(SQL_SELECT + " where response = false");
            busy = false;
        }

        if (resultSet != null) {
            if (resultSet.next())
                messages.add(new Message(resultSet.getString(2)));
        }
        return messages;
    }

    private ResultSet queryAll() throws SQLException {
        ResultSet resultSet = null;
        if (!busy) {
            busy = true;
            createConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(SQL_SELECT,
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            resultSet = preparedStatement.executeQuery();
            rowCount = resultSet.getRow();
            resultSet.beforeFirst();
            busy = false;
        }
        return resultSet;
    }

    private void setMessageDone(String id) throws SQLException {
        if (!busy) {
            busy = true;
            createConnection();
            connection.createStatement().execute(SQL_UPDATE + " response = true where id = " + id);
            System.out.println("Message with ID : " + id + " Successfully Set To Done");
            closeConnection();
            busy = false;
        }
    }

    private void deletePlayer(String id) throws SQLException {
        if (!busy) {
            busy = true;
            createConnection();
            connection.createStatement().execute(SQL_DELETE + " where id = " + id);
            System.out.println("Record Successfully Deleted");
            closeConnection();
            busy = false;
        }
    }

    private void deleteAll() throws SQLException {
        if (!busy) {
            busy = true;
            createConnection();
            connection.createStatement().execute(SQL_DELETE + " where 1 = 1");
            System.out.println("Records Successfully Deleted");
            closeConnection();
            busy = false;
        }
    }

    private void createConnection() throws SQLException {
        if (connection == null)
            connection = DriverManager.getConnection(JDBC_URL);
        statement = connection.createStatement();
    }

    private void closeConnection() throws SQLException {
        if (statement != null)
            statement.close();
        if (connection != null)
            connection.close();
        connection = null;
    }

    private int getRowCount() {
        return rowCount;
    }

    @Override
    public Object call() throws Exception {
        switch (action){
            case "CREATE":
                createDB();
                break;
            case "DROP":
                dropDB();
                break;
            case "INSERT":
                if (message != null)
                    insertTransaction(message);
                break;
            case "DELETE":
                if (this.message != null)
                    deletePlayer(((Message)message).getId());
                break;
            case "SET":
                if (this.message != null)
                    setMessageDone(((Message)message).getId());
                break;
            case "FETCH":
                return queryMessage(((Message)message).getId());
            case "UNDONE":
                return queryMessages();
            case "ALL":
                return queryAll();
        }
        return null;
    }
}
