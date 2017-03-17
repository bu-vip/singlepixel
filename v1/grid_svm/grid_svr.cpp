#include <iostream>
#include <cstdlib>
#include <string>
#include <sstream>
#include <time.h>

typedef unsigned int uint;

typedef struct _CGPPair {
  uint c;
  uint g;
  uint p;
} CGPPair;

typedef struct _SVMArgs {
  float cMin;
  float cMax;
  float cStep;
  float gMin;
  float gMax;
  float gStep;
  float pMin;
  float pMax;
  float pStep;
  char * args;
  char * name;
} SVMArgs;

void runJob(SVMArgs aArgs, char * aQueue);
void runJobs(SVMArgs aArgs, CGPPair aCGPair, char * aQueue);
CGPPair calculateBestCGPPair(SVMArgs aArgs, uint aMaxJobs);

int main(int argc, char *argv[])
{
  if (argc == 14)
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
      args.pMin = atof(argv[argNum++]);
      args.pMax = atof(argv[argNum++]);
      args.pStep = atof(argv[argNum++]);
      args.args = argv[argNum++];
      args.name = argv[argNum++];

      CGPPair best = calculateBestCGPPair(args, maxJobs);
      if (best.c > 0 && best.g > 0 && best.p > 0)
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
    std::cout << "Usage: <queue> <maxJobs> <cMin> <cMax> <cStep> <gMin> <gMax> <gStep> <pMin> <pMax> <pStep> \"<other svm args...>\" <name>" << std::endl;
    std::cout << "Arg count: " << argc << std::endl;
  }

  return 0;
}

//find best combination of # of c & g points for each job to do
CGPPair calculateBestCGPPair(SVMArgs aArgs, uint aMaxJobs)
{
  CGPPair result;
  result.c = -1;
  result.g = -1;
  result.p = -1;
  int bestJobCount = -1;
  bool done = false;
  int cPointCount = (aArgs.cMax - aArgs.cMin) / aArgs.cStep;
  int gPointCount = (aArgs.gMax - aArgs.gMin) / aArgs.gStep;
  int pPointCount = (aArgs.pMax - aArgs.pMin) / aArgs.pStep;
  //simple brute force optimization
  for (int cPointsPerJob = cPointCount; cPointsPerJob >= 1 && !done; cPointsPerJob--)
  {
    for (int gPointsPerJob = gPointCount; gPointsPerJob >= 1 && !done; gPointsPerJob--)
    {
    for (int pPointsPerJob = pPointCount; pPointsPerJob >= 1 && !done; pPointsPerJob--)
    {
      //for simplicity, make all jobs must have same number of points
      if (cPointCount % cPointsPerJob == 0 && gPointCount % gPointsPerJob == 0 && pPointCount % pPointsPerJob == 0)
      {
        int cRanges = cPointCount / cPointsPerJob;
        int gRanges = gPointCount / gPointsPerJob;
        int pRanges = pPointCount / pPointsPerJob;
        int jobCount = cRanges * gRanges * pRanges;
        if (jobCount <= aMaxJobs && jobCount > bestJobCount)
        {
          bestJobCount = jobCount;
          result.c = cPointsPerJob;
          result.g = gPointsPerJob;
          result.p = pPointsPerJob;
          if (bestJobCount == aMaxJobs)
          {
            done = true;
          }
        }
      }
    }
  }
  }

  return result;
}

void runJobs(SVMArgs aArgs, CGPPair aCGPair, char * aQueue)
{
  int cPointCount = (aArgs.cMax - aArgs.cMin) / aArgs.cStep;
  int gPointCount = (aArgs.gMax - aArgs.gMin) / aArgs.gStep;
  int pPointCount = (aArgs.pMax - aArgs.pMin) / aArgs.pStep;
  for (int cPoint = 0; cPoint < cPointCount; cPoint += aCGPair.c)
  {
    for (int gPoint = 0; gPoint < gPointCount; gPoint += aCGPair.g)
    {
    for (int pPoint = 0; pPoint < pPointCount; pPoint += aCGPair.p)
    {
      SVMArgs jobArgs;
      jobArgs.cMin = cPoint * aArgs.cStep + aArgs.cMin;
      jobArgs.cMax = (cPoint + aCGPair.c) * aArgs.cStep + aArgs.cMin;
      jobArgs.cStep = aArgs.cStep;
      jobArgs.gMin = gPoint * aArgs.gStep + aArgs.gMin;
      jobArgs.gMax = (gPoint + aCGPair.g) * aArgs.gStep + aArgs.gMin;
      jobArgs.gStep = aArgs.gStep;
      jobArgs.pMin = pPoint * aArgs.pStep + aArgs.pMin;
      jobArgs.pMax = (pPoint + aCGPair.p) * aArgs.pStep + aArgs.pMin;
      jobArgs.pStep = aArgs.pStep;
      jobArgs.args = aArgs.args;
      jobArgs.name = aArgs.name;
      runJob(jobArgs, aQueue);
  }
}
  }
}

void runJob(SVMArgs aArgs, char * aQueue)
{
  int id = rand();

  //name of the output dir
  std::stringstream outDirSS;
  outDirSS << "out/" << aArgs.name << "/" << id << "/";
  std::string outDir = outDirSS.str();
  //make the output dir
  std::stringstream mkdirCommand;
  mkdirCommand << "mkdir -p " << outDir;
  //std::cout << "Running: " << mkdirCommand.str() << std::endl;
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
  ss << "-N " << aArgs.name << "-" << id;
  //specify job to run
  ss << " -b y \"";
  ss << "python libsvm/gridregression.py -log2c " << aArgs.cMin << "," << aArgs.cMax << "," << aArgs.cStep;
  ss << " -log2g " << aArgs.gMin << "," << aArgs.gMax << "," << aArgs.gStep;
  ss << " -log2p " << aArgs.pMin << "," << aArgs.pMax << "," << aArgs.pStep;
  ss << " -out " << outDir << "out-" << id << ".txt -png " << outDir << "out-" << id << ".png ";
  ss << aArgs.args << "\"";
  std::string jobCommand = ss.str();
  //std::cout << "Running: " << jobCommand << std::endl;
  //system is bad
  system(jobCommand.c_str());
}
