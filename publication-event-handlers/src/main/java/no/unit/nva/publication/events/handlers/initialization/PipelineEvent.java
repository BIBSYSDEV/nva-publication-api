package no.unit.nva.publication.events.handlers.initialization;

public class PipelineEvent {

    private String pipeline;
    private String state;

    public String getPipeline() {
        return pipeline;
    }

    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
