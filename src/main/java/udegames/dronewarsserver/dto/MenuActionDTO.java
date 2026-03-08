package udegames.dronewarsserver.dto;

public class MenuActionDTO {
    private boolean success;
    private String message;
    private String nextScreen;
    private Object data;

    public MenuActionDTO(boolean success, String message, String nextScreen, Object data) {
        this.success = success;
        this.message = message;
        this.nextScreen = nextScreen;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getNextScreen() {
        return nextScreen;
    }

    public Object getData() {
        return data;
    }
}