package org.assimbly.dil.validation.beans.script;

public class ScriptDto {

    private String language;
    private String script;

    @SuppressWarnings("unused")
    protected ScriptDto() {}

    public ScriptDto(String language, String script) {
        this.language = language;
        this.script = script;
    }

    public String getLanguage() {
        return language;
    }

    @SuppressWarnings("unused")
    protected void setLanguage(String language) {
        this.language = language;
    }

    public String getScript() {
        return script;
    }

    @SuppressWarnings("unused")
    protected void setScript(String script) {
        this.script = script;
    }
}
