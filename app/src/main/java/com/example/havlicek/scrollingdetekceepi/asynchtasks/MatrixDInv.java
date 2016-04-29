package com.example.havlicek.scrollingdetekceepi.asynchtasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.example.havlicek.scrollingdetekceepi.uithread.ServiceDetekce;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.List;

/**
 * Created by Ondřej on 28. 4. 2016.
 */
public class MatrixDInv extends AsyncTask<Void, Integer, RealMatrix> {
    ServiceDetekce s;
    public MatrixDInv(ServiceDetekce c){
        this.s = c;
    }

    protected void onProgressUpdate(Integer... progress) {

    }

    protected void onPostExecute(RealMatrix result) {
        Toast.makeText(s, "Matrix computed", Toast.LENGTH_SHORT).show();
        Log.d("matrix", "finished");
        s.matrix = result;
    }

    @Override
    protected RealMatrix doInBackground(Void [] params) {
        int N = 256; // velikost matice
        double [][] matrix = new double[N][N];

        int num;
        for (int i = 0; i < N; i++){
            for (int j = 0; j < N; j++){
                num = 0;
                if (i == 0){
                    switch (j){
                        case 0: num+=9000+1;break;
                        case 1: num+=-2*9000;break;
                        case 2: num+=9000;break;
                    }
                } else if(i == 1) {
                    switch (j){
                        case 0: num+=-2*9000;break;
                        case 1: num+=1+5*9000;break;
                        case 2: num+=-4*9000;break;
                        case 3: num+=9000;break;
                    }
                } else if (i == N-2){
                    switch (N-j){
                        case 0: num+=-2*9000;break;
                        case 1: num+=1+5*9000;break;
                        case 2: num+=-4*9000;break;
                        case 3: num+=9000;break;
                    }
                } else if (i == N-1){
                    switch (N-j){
                        case 0: num+=9000+1;break;
                        case 1: num+=-2*9000;break;
                        case 2: num+=9000;break;
                    }
                } else {
                    if (i == j)num += 1 + 6*9000;
                    else if (i-2==j||i+2==j)num += 9000;
                    else if (i-1==j||i+1==j)num += -4*9000;
                }
                matrix[i][j] = num;
            }
        }
        double [][] val = matrix;
        Log.d("matrix","radka:0; "+val[0][0]+" "+val[0][1]+" "+val[0][2]+" "+val[0][3]+" "+val[0][4]+" "+val[0][5]);
        Log.d("matrix","radka:1; "+val[1][0]+" "+val[1][1]+" "+val[1][2]+" "+val[1][3]+" "+val[1][4]+" "+val[1][5]);
        Log.d("matrix","radka:2; "+val[2][0]+" "+val[2][1]+" "+val[2][2]+" "+val[2][3]+" "+val[2][4]+" "+val[2][5]);
        Log.d("matrix","radka:3; "+val[3][0]+" "+val[3][1]+" "+val[3][2]+" "+val[3][3]+" "+val[3][4]+" "+val[3][5]);


        RealMatrix m = new Array2DRowRealMatrix(matrix);
        RealMatrix inv = new LUDecomposition(m).getSolver().getInverse();
        inv = inv.scalarMultiply(-1);
        // idecteni jednotkove matice
        for (int i = 0; i < N ; i++){
            inv.addToEntry(i,i,1);
        }
        return inv;
    }
}
