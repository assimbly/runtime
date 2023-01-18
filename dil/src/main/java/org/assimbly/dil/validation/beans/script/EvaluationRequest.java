package org.assimbly.dil.validation.beans.script;

public class EvaluationRequest {

    private ExchangeDto exchange;
    private ScriptDto script;

    @SuppressWarnings("unused")
    protected EvaluationRequest() {}

    public EvaluationRequest(ExchangeDto exchange, ScriptDto script) {
        this.exchange= exchange;
        this.script = script;
    }
    public ExchangeDto getExchange() {
        return exchange;
    }

    @SuppressWarnings("unused")
    protected void setExchange(ExchangeDto exchange) {
        this.exchange = exchange;
    }

    public ScriptDto getScript() {
        return script;
    }

    @SuppressWarnings("unused")
    protected void setScript(ScriptDto script) {
        this.script = script;
    }
}
