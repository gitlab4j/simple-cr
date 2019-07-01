package org.gitlab4j.simplecr.beans;

public class AppResponse<T> {

    public static enum Status {
        OK, FAILED, NO_ACTION;
    }

    private boolean success;
    private Status status;
    private String statusText;
    private T data;

    public AppResponse(Status status, T data) {
        this.data = data;
        this.status = status;
        this.success = !Status.FAILED.equals(status);
    }

    public AppResponse(Status status, String statusText, T data) {
        this.data = data;
        this.statusText = statusText;
        this.status = status;
        this.success = !Status.FAILED.equals(status);
    }

    public boolean getSuccess() {
        return success;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {

        this.status = status;
        this.success = Status.OK.equals(status);
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    /**
     *
     * @param success
     * @param message
     * @return
     */
    public static final AppResponse<String> getMessageResponse(boolean success, String message) {
        Status status = success ? Status.OK : Status.FAILED;
        return new AppResponse<String>(status, message, message);
    }

    public static final AppResponse<String> getMessageResponse(Status status, String message) {
        return new AppResponse<String>(status, message, message);
    }

    public static final <T> AppResponse<T>getDataResponse(boolean success, T data) {
        Status status = success ? Status.OK : Status.FAILED;
        return new AppResponse<T>(status, data);
    }

    public static final <T> AppResponse<T> getResponse(Status status, String message, T data) {
        return new AppResponse<T>(status, message, data);
    }
}
