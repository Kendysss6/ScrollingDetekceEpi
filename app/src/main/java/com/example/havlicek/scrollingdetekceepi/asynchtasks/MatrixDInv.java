package com.example.havlicek.scrollingdetekceepi.asynchtasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.example.havlicek.scrollingdetekceepi.R;
import com.example.havlicek.scrollingdetekceepi.uithread.ServiceDetekce;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by Ondřej on 28. 4. 2016.
 */
public class MatrixDInv extends AsyncTask<Void, Integer, RealMatrix> {
    ServiceDetekce s;
    private double sum = 0;
    private double norm = 0;
    public MatrixDInv(ServiceDetekce c){
        this.s = c;
    }

    protected void onProgressUpdate(Integer... progress) {

    }

    protected void onPostExecute(RealMatrix result) {
        Toast.makeText(s, "Matrix computed", Toast.LENGTH_SHORT).show();
        Log.d("matrix", "finished");
        Log.d("matrix", "inv.suma " + sum); // uz moc matlabu neodpovida Matlab:534.5659
        Log.d("matrix","inv.norma" + norm); // Matlab 15.7628
        s.matrix = result;
    }

    @Override
    protected RealMatrix doInBackground(Void [] params) {
       /* int N = 256; // velikost matice
        double [][] matrix = new double[N][N];

        int num;
        for (int i = 0; i < N; i++){
            for (int j = 0; j < N; j++){
                num = 0;
                if (i == 0){
                    switch (j){
                        case 0: num+=90000+1;break;
                        case 1: num+=-2*90000;break;
                        case 2: num+=90000;break;
                    }
                } else if(i == 1) {
                    switch (j){
                        case 0: num+=-2*90000;break;
                        case 1: num+=1+5*90000;break;
                        case 2: num+=-4*90000;break;
                        case 3: num+=90000;break;
                    }
                } else if (i == N-2){
                    switch (N-j){
                        case 0: num+=-2*90000;break;
                        case 1: num+=1+5*90000;break;
                        case 2: num+=-4*90000;break;
                        case 3: num+=90000;break;
                    }
                } else if (i == N-1){
                    switch (N-j){
                        case 0: num+=90000+1;break;
                        case 1: num+=-2*90000;break;
                        case 2: num+=90000;break;
                    }
                } else {
                    if (i == j)num += 1 + 6*90000;
                    else if (i-2==j||i+2==j)num += 90000;
                    else if (i-1==j||i+1==j)num += -4*90000;
                }
                matrix[i][j] = num;
            }
        }


        RealMatrix m = new Array2DRowRealMatrix(matrix);
        //for (int i = 0; i < N; i++) { je to spravne podle matlabu
            //for (int j = 0; j < N; j++) {
              //  sum += Math.abs(matrix[i][j]);
            //}
        //}


        Matrix jama = new Matrix(matrix);
        //RealMatrix inv = new LUDecomposition(m).getSolver().getInverse();
        Matrix inv = jama.inverse();
        inv.times(-1);


        for (int i = 0; i < N ; i++){
            inv.set(i,i,inv.get(i,i)+1);
        }

        //inv = inv.scalarMultiply(-1);
        // idecteni jednotkove matice
        //this.norm = inv.getFrobeniusNorm();
       // matrix = inv.getData();
       // for (int i = 0; i < N; i++) {
        //    for (int j = 0; j < N; j++) {
        //        sum += Math.abs(matrix[i][j]);
        //    }
        }
        matrix = inv.getArray();
        int l = matrix.length-1;
        Log.d("matrix","inv "+matrix[l-2][l-5]+" "+matrix[l-2][l-4]+" "+matrix[l-2][l-3]+" "+matrix[l-2][l-2]+" "+matrix[l-2][l-1]+" "+matrix[l-2][l]);
        Log.d("matrix","inv "+matrix[l-1][l-5]+" "+matrix[l-1][l-4]+" "+matrix[l-1][l-3]+" "+matrix[l-1][l-2]+" "+matrix[l-1][l-1]+" "+matrix[l-1][l]);
        Log.d("matrix","inv "+matrix[l][l-5]+" "+matrix[l][l-4]+" "+matrix[l][l-3]+" "+matrix[l][l-2]+" "+matrix[l][l-1]+" "+matrix[l][l]);
        return new Array2DRowRealMatrix(inv.getArray());
        */

        String [] array = s.getResources().getStringArray(R.array.matrix);
        double [][] matrix = new double[array.length][array.length];
        for (int i = 0; i < array.length; i++){
            try {
                JSONArray jsonArray = new JSONArray(array[i]);
                for (int j = 0; j < array.length; j++){
                    matrix[i][j] = jsonArray.getDouble(j);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        int l = matrix.length-1;
        Log.d("matrix","inv "+matrix[l-2][l-5]+" "+matrix[l-2][l-4]+" "+matrix[l-2][l-3]+" "+matrix[l-2][l-2]+" "+matrix[l-2][l-1]+" "+matrix[l-2][l]);
        Log.d("matrix","inv "+matrix[l-1][l-5]+" "+matrix[l-1][l-4]+" "+matrix[l-1][l-3]+" "+matrix[l-1][l-2]+" "+matrix[l-1][l-1]+" "+matrix[l-1][l]);
        Log.d("matrix","inv "+matrix[l][l-5]+" "+matrix[l][l-4]+" "+matrix[l][l-3]+" "+matrix[l][l-2]+" "+matrix[l][l-1]+" "+matrix[l][l]);

        return new Array2DRowRealMatrix(matrix);
    }
}
