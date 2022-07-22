package de.offrange.client.models;

/**
 * Model class that implements the {@link IModel} interface. This model is used for the code evaluation.
 * This model is serialized and deserialized by {@link com.google.gson.Gson}.
 */
public class CodeCheckModel implements IModel {

    private String code;

    private boolean isCodeCorrect;

    /**
     * @return the code that was passed with {@link #setCode(String)}
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets a code to enable the connection between the client and the server.
     * @param code the code
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @return true if the server evaluated the passed code as correct, false otherwise.
     */
    public boolean isCodeCorrect() {
        return isCodeCorrect;
    }
}
