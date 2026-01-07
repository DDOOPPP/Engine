package org.gi.storage;

public class MySQLSetting {
    private String host;
    private int port;
    private String user;
    private String password;
    private String database;

    private long connectionTimeout; //pool에서 Connection을 가져오는 시간 //2000mx
    private long idleTimeout = 0; //일하지않는 Pool의 유지시간 0으로
    private long maxLifetime= 0; //커넥션 풀에 살아있을 수 있는 커넥션의 최대 시간 0으로
    private long validationTimeout; //커넥션이 유효한지 검사시 사용하는 타임아웃 //500ms
    private long keepAliveTimeout = 30000; //30000ms

    private int maximumPoolSize; //pool에서 유지시킬 최대 커넥션의 갯수

    public MySQLSetting(String host, int port, String user, String password, String database) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.database = database;
    }

    public MySQLSetting(String host, int port, String user, String password, String database, long connectionTimeout, long idleTimeout, long maxLifetime, long validationTimeout, long keepAliveTimeout, int maximumPoolSize) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.database = database;
        this.connectionTimeout = connectionTimeout;
        this.idleTimeout = idleTimeout;
        this.maxLifetime = maxLifetime;
        this.validationTimeout = validationTimeout;
        this.keepAliveTimeout = keepAliveTimeout;
        this.maximumPoolSize = maximumPoolSize;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public long getMaxLifetime() {
        return maxLifetime;
    }

    public void setMaxLifetime(long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    public long getValidationTimeout() {
        return validationTimeout;
    }

    public void setValidationTimeout(long validationTimeout) {
        this.validationTimeout = validationTimeout;
    }

    public long getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public void setKeepAliveTimeout(long keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public String getURL(){
        return "jdbc:mysql://"+host+":"+port+"/"+database;
    }
}
