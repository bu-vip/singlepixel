# Demo
Single pixel sensor localization demo.

## Data Collection
Before you can run the demo, you'll need to collect some data to train the model.
Follow the instructions [here](/src/main/java/edu/bu/vip/singlepixel/multikinect) to setup the MultiKinect system.

The current algorithm uses background subtraction, so you will need to capture at least one background sample in order for the algorithm to work properly.
When you capture a background sample, there are two things you need to do:
1. Make sure no one is in the room
2. Put "background" in the recording name, e.g. "background1"

The NN training script expects the background recording to have "background" in the name and will automatically handle separating the background and normal recordings.

## Training the NN
Once you have collected some data, the next step is to train the NN model.
The script for training the model is [here](/src/main/python/tensorflow/algorithm.py).
You'll need to install some python libraries to run the script: tensorflow, matplotlib, numpy (there may be more)

To run the script, you'll need to specify some parameters:
```bash
python algorithm.py \
  --model_name <model-name> \
  --out_dir <out-dir, e.g. "/home/doug/Desktop/multikinect/models"> \
  --session_dir <session-dir, e.g. "/home/doug/Desktop/multikinect/sessions"> \
  --num_sensors <num-sensors, e.g. 12> \
  --ids <ids, separated with a space e.g. "446778253 567968358"> \
  --epochs <num-epochs e.g. 10000>
```
You can also run the script with "--help" to get more information.

The script will compute a hash of the parameters and add it to the end of the model name.
This prevents you from accidentally overwriting models when you're tweaking parameters.

> If for some reason the NN ends up with very bad performance at the end of training, e.g. > .3, rerun the script.

## Running Demo
After you've trained the model, you're ready to run the demo.

To run the demo, run the following command:
```bash
bazel run //src/main/java/edu/bu/vip/singlepixel/demo:main \
  --multikinect_dir <dir, e.g. "/home/doug/Desktop/multikinect"> \
  --model <model-file, e.g. "/home/doug/models/my_model_1has13.pb"> \
  --recordings_dir <recordings-dir, e.g. "/home/doug/Desktop/demo_recordings"> \
  --calibration_id <id, e.g. 471923155>
```
> The calibration id you specify should match the one you used to collect the data.

Once you've started the program, you can connect the Kinects as you would normally do with the regular MultiKinect system.

You'll also need to start the web console:
```bash
cd src/main/javascript/demo/
npm install
npm start
```
Then go to [http://localhost:3000](http://localhost:3000) in your browser.

## Extending
If you would like to try a different ML algorithm (that uses the same feature vector), it should be pretty straightforward to add it.
You will need to implement the
[LocationPreditor](/src/main/java/edu/bu/vip/singlepixel/demo/LocationPredictor.java)
interface and then need to initialize it
[here](/src/main/java/edu/bu/vip/singlepixel/demo/Demo.java#L79).

You'll probably also want to add a
[command line arg](/src/main/java/edu/bu/vip/singlepixel/demo/MainArgs.java)
to toggle between different algorithms.
