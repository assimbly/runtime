package org.assimbly.dil.validation.beans.script;

public class EvaluationResponse {

    private ExchangeDto exchange;
    private String result;
    private int code;

    @SuppressWarnings("unused")
    protected EvaluationResponse() {}

    public EvaluationResponse(ExchangeDto exchange, String result, int code) {
        this.exchange = exchange;
        this.result = result;
        this.code = code;
    }

    public ExchangeDto getExchange() {
        return exchange;
    }

    @SuppressWarnings("unused")
    protected void setExchange(ExchangeDto exchange) {
        this.exchange = exchange;
    }

    public String getResult() {
        return result;
    }

    @SuppressWarnings("unused")
    protected void setResult(String result) {
        this.result = result;
    }

    public int getCode() {
        return code;
    }

    @SuppressWarnings("unused")
    public void setCode(int code) {
        this.code = code;
    }
}
