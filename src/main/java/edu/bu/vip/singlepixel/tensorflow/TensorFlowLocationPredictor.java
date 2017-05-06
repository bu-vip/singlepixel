package edu.bu.vip.singlepixel.tensorflow;

import com.google.common.collect.ImmutableList;
import edu.bu.vip.multikinect.Protos.Position;
import edu.bu.vip.singlepixel.demo.LocationPredictor;

public class TensorFlowLocationPredictor implements LocationPredictor {

  public static class Builder {

    private String modelPath;
    private String inputNodeName;
    private String outputNodeName;

    public Builder setModelPath(String modelPath) {
      this.modelPath = modelPath;
      return this;
    }

    public Builder setInputNodeName(String inputNodeName) {
      this.inputNodeName = inputNodeName;
      return this;
    }

    public Builder setOutputNodeName(String outputNodeName) {
      this.outputNodeName = outputNodeName;
      return this;
    }

    public TensorFlowLocationPredictor build() {
      return new TensorFlowLocationPredictor(modelPath, inputNodeName, outputNodeName);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private final TensorFlowInterface tensorFlowInterface;
  private final String inputNodeName;
  private final String outputNodeName;

  public TensorFlowLocationPredictor(String modelPath, String inputNodeName,
      String outputNodeName) {
    this.tensorFlowInterface = new TensorFlowInterface(modelPath);
    this.inputNodeName = inputNodeName;
    this.outputNodeName = outputNodeName;
  }

  @Override
  public ImmutableList<Position> predict(float[] feature) {
    // Input feature vector into graph
    tensorFlowInterface.feed(inputNodeName, feature, 1, feature.length);
    // Run graph, calculate output node
    tensorFlowInterface.run(ImmutableList.of(outputNodeName));
    // Get output
    float[] position = new float[2];
    tensorFlowInterface.fetch(outputNodeName, position);

    Position.Builder builder = Position.newBuilder();
    builder.setX(position[0]);
    builder.setY(0);
    builder.setZ(position[1]);
    return ImmutableList.of(builder.build());
  }
}
