#include <iostream>
#include <cstdlib>
#include <string>
#include <sstream>
#include <time>

typedef unsigned int uint;

typedef struct _CGPair {
  uint c;
  uint g;
} CGPair;

typedef struct _SVMArgs {
  float cMin;
  float cMax;
  float cStep;
  float gMin;
  float gMax;
  float gStep;
  char * args;
  char * name;
} SVMArgs;

void runJob(SVMArgs aArgs, char * aQueue);
void runJobs(SVMArgs aArgs, CGPair aCGPair, char * aQueue);
CGPair calculateBestCGPair(SVMArgs aArgs, uint aMaxJobs);

int main(int argc, char *argv[])
{
  if (argc == 11)
  {
      srand(time(NULL));
      uint argNum = 1;
      char * queue = argv[argNum++];
      uint maxJobs = atoi(argv[argNum++]);
      SVMArgs args;
      args.cMin = atof(argv[argNum++]);
      args.cMax = atof(argv[argNum++]);
      args.cStep = atof(argv[argNum++]);
      args.gMin = atof(argv[argNum++]);
      args.gMax = atof(argv[argNum++]);
      args.gStep = atof(argv[argNum++]);
      args.args = argv[argNum++];
      args.name = argv[argNum++];

      CGPair best = calculateBestCGPair(args, maxJobs);
      if (best.c > 0 && best.g > 0)
      {
          runJobs(args, best, queue);
      }
      else
      {
        std::cout << "An error occured breaking up jobs" << std::endl;
      }
  }
  else
  {
    std::cout << "Usage: <queue> <maxJobs> <cMin> <cMax> <cStep> <gMin> <gMax> <gStep> \"<other svm args...>\" <name>" << std::endl;
  }

  return 0;
}

//find best combination of # of c & g points for each job to do
CGPair calculateBestCGPair(SVMArgs aArgs, uint aMaxJobs)
{
  CGPair result;
  result.c = -1;
  result.g = -1;
  int bestJobCount = -1;
  bool done = false;
  int cPointCount = (aArgs.cMax - aArgs.cMin) / aArgs.cStep;
  int gPointCount = (aArgs.gMax - aArgs.gMin) / aArgs.gStep;
  //simple brute force optimization
  for (int cPointsPerJob = cPointCount; cPointsPerJob >= 1 && !done; cPointsPerJob--)
  {
    for (int gPointsPerJob = gPointCount; gPointsPerJob >= 1 && !done; gPointsPerJob--)
    {
      //for simplicity, make all jobs must have same number of points
      if (cPointCount % cPointsPerJob == 0 && gPointCount % gPointsPerJob == 0)
      {
        int cRanges = cPointCount / cPointsPerJob;
        int gRanges = gPointCount / gPointsPerJob;
        int jobCount = cRanges * gRanges;
        if (jobCount <= aMaxJobs && jobCount > bestJobCount)
        {
          bestJobCount = jobCount;
          result.c = cPointsPerJob;
          result.g = gPointsPerJob;
          if (bestJobCount == aMaxJobs)
          {
            done = true;
          }
        }
      }
    }
  }

  return result;
}

void runJobs(SVMArgs aArgs, CGPair aCGPair, char * aQueue)
{
  int cPointCount = (aArgs.cMax - aArgs.cMin) / aArgs.cStep;
  int gPointCount = (aArgs.gMax - aArgs.gMin) / aArgs.gStep;
  for (int cPoint = 0; cPoint < cPointCount; cPoint += aCGPair.c)
  {
    for (int gPoint = 0; gPoint < gPointCount; gPoint += aCGPair.g)
    {
      SVMArgs jobArgs;
      jobArgs.cMin = cPoint * aArgs.cStep + aArgs.cMin;
      jobArgs.cMax = (cPoint + aCGPair.c) * aArgs.cStep + aArgs.cMin;
      jobArgs.cStep = aArgs.cStep;
      jobArgs.gMin = gPoint * aArgs.gStep + aArgs.gMin;
      jobArgs.gMax = (gPoint + aCGPair.g) * aArgs.gStep + aArgs.gMin;
      jobArgs.gStep = aArgs.gStep;
      jobArgs.args = aArgs.args;
      jobArgs.name = aArgs.name;
      runJob(jobArgs, aQueue);
    }
  }
}

void runJob(SVMArgs aArgs, char * aQueue)
{
  int id = rand();

  //name of the output dir
  std::stringstream outDirSS;
  outDirSS << outDir.name << "/" << id << "/";
  std::string outDir = outDirSS.str();
  //make the output dir
  std::stringstream mkdirCommand;
  mkdirCommand << "mkdir -p " << outDir;
  std::cout << "Running: " << mkdirCommand.str();
  //system is bad
  system(mkdirCommand.str().c_str());

  //create the job command
  std::stringstream ss;
  //join error and output into single files
  ss << "qsub -j y ";
  //set output file name
  ss << "-o " << outDir << "out-" << id << ".log ";
  //specify queue name
  ss << "-q " << aQueue;
  //set current working dir
  ss << " -cwd ";
  //give job a name
  ss << "-n " << aArgs.name << "-" << id;
  //specify job to run
  ss << " -b y \"";
  ss << "python libsvm/grid.py -log2c " << aArgs.cMin << "," << aArgs.cMax << "," << aArgs.cStep;
  ss << " -log2g " << aArgs.gMin << "," << aArgs.gMax << "," << aArgs.gStep;
  ss << " -out " << outDir << "out-" << id << ".txt -png " << outDir << "out-" << id << ".png ";
  ss << aArgs.args << "\"";
  std::string jobCommand = ss.str();
  std::cout << "Running: " << command << std::endl;
  //system is bad
  system(jobCommand.c_str());
}
