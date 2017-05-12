/*
 * Copyright (c) 2009-2015, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.bu.vip.singlepixel.filter;

import static org.ejml.ops.CommonOps.addEquals;
import static org.ejml.ops.CommonOps.mult;
import static org.ejml.ops.CommonOps.multTransA;
import static org.ejml.ops.CommonOps.multTransB;
import static org.ejml.ops.CommonOps.subtract;
import static org.ejml.ops.CommonOps.subtractEquals;

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.interfaces.linsol.LinearSolver;

/**
 * Taken from: https://github.com/lessthanoptimal/ejml/blob/v0.27/examples/src/org/ejml/example/KalmanFilter.java
 */
public class KalmanFilter {

  // kinematics description
  private DenseMatrix64F F, Q, H;

  // system state estimate
  private DenseMatrix64F x, P;

  // these are predeclared for efficiency reasons
  private DenseMatrix64F a, b;
  private DenseMatrix64F y, S, S_inv, c, d;
  private DenseMatrix64F K;

  private LinearSolver<DenseMatrix64F> solver;

  public void configure(DenseMatrix64F F, DenseMatrix64F Q, DenseMatrix64F H) {
    this.F = F;
    this.Q = Q;
    this.H = H;

    int dimenX = F.numCols;
    int dimenZ = H.numRows;

    a = new DenseMatrix64F(dimenX, 1);
    b = new DenseMatrix64F(dimenX, dimenX);
    y = new DenseMatrix64F(dimenZ, 1);
    S = new DenseMatrix64F(dimenZ, dimenZ);
    S_inv = new DenseMatrix64F(dimenZ, dimenZ);
    c = new DenseMatrix64F(dimenZ, dimenX);
    d = new DenseMatrix64F(dimenX, dimenZ);
    K = new DenseMatrix64F(dimenX, dimenZ);

    x = new DenseMatrix64F(dimenX, 1);
    P = new DenseMatrix64F(dimenX, dimenX);

    // covariance matrices are symmetric positive semi-definite
    solver = LinearSolverFactory.symmPosDef(dimenX);
  }

  public void setState(DenseMatrix64F x, DenseMatrix64F P) {
    this.x.set(x);
    this.P.set(P);
  }

  public void predict() {

    // x = F x
    mult(F, x, a);
    x.set(a);

    // P = F P F' + Q
    mult(F, P, b);
    multTransB(b, F, P);
    addEquals(P, Q);
  }

  public void update(DenseMatrix64F z, DenseMatrix64F R) {
    // y = z - H x
    mult(H, x, y);
    subtract(z, y, y);

    // S = H P H' + R
    mult(H, P, c);
    multTransB(c, H, S);
    addEquals(S, R);

    // K = PH'S^(-1)
    if (!solver.setA(S)) {
      throw new RuntimeException("Invert failed");
    }
    solver.invert(S_inv);
    multTransA(H, S_inv, d);
    mult(P, d, K);

    // x = x + Ky
    mult(K, y, a);
    addEquals(x, a);

    // P = (I-kH)P = P - (KH)P = P-K(HP)
    mult(H, P, c);
    mult(K, c, b);
    subtractEquals(P, b);
  }

  public DenseMatrix64F getState() {
    return x;
  }

  public DenseMatrix64F getCovariance() {
    return P;
  }
}
