# python

First, generate the necessary protocol buffer files by running the script from the root of the repo:
```bash
python scripts/gen_py_protos.py
```

The main functions for reading data are in `read_session.py`. Specifically, `combine_data` will link
the kinect and sensor data together. See `train_nn.py` for an example.
