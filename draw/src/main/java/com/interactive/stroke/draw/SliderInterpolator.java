package com.interactive.stroke.draw;


import org.apache.commons.math3.analysis.polynomials.PolynomialFunctionLagrangeForm;


/**
 * Created by francois on 16/09/13.
 */
public class SliderInterpolator {


    static double[] x1 = { 0, 5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };
    static double[] y1 = { 0, 2.5, 5, 8, 12, 16, 20, 30, 40, 50, 70, 90 };
    static double[] y2 = { 0, 2.8, 6, 10, 14, 19, 24, 35, 46, 57, 78, 99 };
    static  PolynomialFunctionLagrangeForm lagrangeMin = new PolynomialFunctionLagrangeForm(
            x1, y1);
    static  PolynomialFunctionLagrangeForm lagrangeMax = new PolynomialFunctionLagrangeForm(
            x1, y2);


    public static float getBrushSizeMin(int progress, float DENSITY) {
        return (float)(0.5f+ lagrangeMin.value(progress)*DENSITY) ;
    }

    public static float getBrushSizeMax(int progress, float DENSITY) {
        return (float)(0.5f+ lagrangeMax.value(progress)*DENSITY) ;
    }
}

