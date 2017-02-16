package org.hitlabnz.sensor_fusion_demo;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.hitlabnz.sensor_fusion_demo.orientationProvider.OrientationProvider;
import org.hitlabnz.sensor_fusion_demo.representation.Quaternion;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Class that implements the rendering of a cube with the current rotation of the device that is provided by a
 * OrientationProvider
 * 
 * @author Alexander Pacha
 * 
 */
public class CubeRenderer implements GLSurfaceView.Renderer {
    /**
     * The colour-cube that is drawn repeatedly
     */
    private Cube mCube;

    static public class DeleteFileDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Delete the file?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Log.i("Dialog","DELETE!");
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Log.i("Dialog","Cancel.");
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }
    private DeleteFileDialogFragment mDeleteDialog = new DeleteFileDialogFragment();

    /**
     * The current provider of the device orientation.
     */
    private OrientationProvider orientationProvider = null;
    private Quaternion quaternion = new Quaternion();
    private boolean mScanOn = false;
    private File mMriderFile;
    private int numpts = 0;

    public boolean isScanning() { return mScanOn; }

    public void startScanning() {
        if ( !isExternalStorageWritable() ) {
            Log.e("Exception","External storage not writable.");
            return;
        }
        mScanOn = true;
        mCube.activate();
        Log.i("Scanning","START");
        writeToFile("CURVE",true/* append */);
        Log.i("Scanning","CURVE");
        addSegment();
    }

    public void addSegment() {
        writeToFile("SEGMENT",true/* append */);
        Log.i("Scanning","SEGMENT");
    }

    public void stopScanning() {
        mScanOn = false;
        mCube.deactivate();
        Log.i("Scanning","STOP");
    }

    /**
     * Initialises a new CubeRenderer
     */
    public CubeRenderer(Context context) {
        // init the cube
        mCube = new Cube();
        // open Morphorider file
        mMriderFile = new File( context.getExternalFilesDir(null), "mrider.txt");
        if(mMriderFile.exists()) {
            //mDeleteDialog.show();
            //mMriderFile.delete();
        }
        // clear the file
        writeToFile("MORPHORIDER NETWORK",false/*do not append, erase*/);
    }

    /**
     * Sets the orientationProvider of this renderer. Use this method to change which sensor fusion should be currently
     * used for rendering the cube. Simply exchange it with another orientationProvider and the cube will be rendered
     * with another approach.
     * 
     * @param orientationProvider The new orientation provider that delivers the current orientation of the device
     */
    public void setOrientationProvider(OrientationProvider orientationProvider) {
        this.orientationProvider = orientationProvider;
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
            return true;
        return false;
    }

    private void writeToFile(String data, boolean append) {
        try {
            BufferedWriter writer = new BufferedWriter(
                    new FileWriter(mMriderFile,append));
            writer.write(data+"\n");
            writer.close();
        }
        catch (IOException e) {
            Log.e("Exception","File write failed: "+e.toString());
        }
    }

    /**
     * Perform the actual rendering of the cube for each frame
     * 
     * @param gl The surface on which the cube should be rendered
     */
    public void onDrawFrame(GL10 gl) {

        // clear screen
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

        // set-up modelview matrix
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        if (showCubeInsideOut) {
            float dist = 3;
            gl.glTranslatef(0, 0, -dist);

            if (orientationProvider != null) {
                // All Orientation providers deliver Quaternion as well as rotation matrix.
                // Use your favourite representation:

                // Get the rotation from the current orientationProvider as rotation matrix
                //gl.glMultMatrixf(orientationProvider.getRotationMatrix().getMatrix(), 0);

                // Get the rotation from the current orientationProvider as quaternion
                orientationProvider.getQuaternion(quaternion);
                gl.glRotatef((float) (2.0f * Math.acos(quaternion.getW()) * 180.0f / Math.PI), quaternion.getX(), quaternion.getY(), quaternion.getZ());
            }

            // draw our object
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

            mCube.draw(gl);
        } else {

            if (orientationProvider != null) {
                // All Orientation providers deliver Quaternion as well as rotation matrix.
                // Use your favourite representation:

                // Get the rotation from the current orientationProvider as rotation matrix
                //gl.glMultMatrixf(orientationProvider.getRotationMatrix().getMatrix(), 0);

                // Get the rotation from the current orientationProvider as quaternion
                orientationProvider.getQuaternion(quaternion);
                gl.glRotatef((float) (2.0f * Math.acos(quaternion.getW()) * 180.0f / Math.PI), quaternion.getX(), quaternion.getY(), quaternion.getZ());
            }

            float dist = 3;
            drawTranslatedCube(gl, 0, 0, -dist);
            drawTranslatedCube(gl, 0, 0, dist);
            drawTranslatedCube(gl, 0, -dist, 0);
            drawTranslatedCube(gl, 0, dist, 0);
            drawTranslatedCube(gl, -dist, 0, 0);
            drawTranslatedCube(gl, dist, 0, 0);
        }

        if( mScanOn ) {
            long seconds = System.currentTimeMillis();
            String outputString = String.format("%16d ",seconds);
            outputString += quaternion.toString();
            writeToFile(outputString,true/*append*/);
            numpts++;
            // Output stuff to log
            Log.i("Time+Quat", outputString);
        }
        // draw our object
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

        mCube.draw(gl);
    }

    /**
     * Draws a translated cube
     * 
     * @param gl the surface
     * @param translateX x-translation
     * @param translateY y-translation
     * @param translateZ z-translation
     */
    private void drawTranslatedCube(GL10 gl, float translateX, float translateY, float translateZ) {
        gl.glPushMatrix();
        gl.glTranslatef(translateX, translateY, translateZ);

        // draw our object
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

        mCube.draw(gl);
        gl.glPopMatrix();
    }

    /**
     * Update view-port with the new surface
     * 
     * @param gl the surface
     * @param width new width
     * @param height new height
     */
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // set view-port
        gl.glViewport(0, 0, width, height);
        // set projection matrix
        float ratio = (float) width / height;
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // dither is enabled by default, we don't need it
        gl.glDisable(GL10.GL_DITHER);
        // clear screen in black
        gl.glClearColor(0, 0, 0, 1);
    }

    /**
     * Flag indicating whether you want to view inside out, or outside in
     */
    private boolean showCubeInsideOut = true;

    /**
     * Toggles whether the cube will be shown inside-out or outside in.
     */
    public void toggleShowCubeInsideOut() {
        this.showCubeInsideOut = !showCubeInsideOut;
    }
}
