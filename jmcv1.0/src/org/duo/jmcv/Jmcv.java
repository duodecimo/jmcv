package org.duo.jmcv;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
import static org.bytedeco.javacpp.opencv_core.cvLoad;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvRectangle;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;
import org.bytedeco.javacv.FrameGrabber;
import org.duo.jmcv.utilities.CascadeName;
/**
 * test
 *
 * @author normenhansen
 */
public class Jmcv {

    private IplImage iplImage; // we can get a BufferedImage here
    private IplImage grayImage;
    private CvRect cvRect; // we can get x,y,w,h (rectangle definition) where eyes were detected
    private Dimension detectionPoint;
    private Dimension detectionArea;
    private CvMemStorage storage;
    private FrameGrabber grabber;
    private CvHaarClassifierCascade classifier;

    public Jmcv() {
        try {
            classifier = new CvHaarClassifierCascade(cvLoad(getClass().getClassLoader().
            getResource(CascadeName.CASCADE_FRONTALFACE_ALT2).getPath()));
            if (classifier.isNull()) {
                System.err.println("Error loading classifier file \"" + 
                        getClass().getClassLoader().
            getResource(CascadeName.CASCADE_FRONTALFACE_ALT2).getPath() + "\".");
                System.exit(1);
            }
            storage = CvMemStorage.create();
            try {
                // try camera 0
                grabber = FrameGrabber.createDefault(0);
                grabber.start();
                iplImage = grabber.grab();
            } catch (FrameGrabber.Exception exception) {
                // if camera 0 fails, try camera 1
                grabber = FrameGrabber.createDefault(1);
                grabber.start();
                iplImage = grabber.grab();
            }
            grayImage = IplImage.create(iplImage.width(), iplImage.height(), IPL_DEPTH_8U, 1);
            detectionArea = new Dimension();
            detectionPoint = new Dimension();
        } catch (FrameGrabber.Exception ex) {
            Logger.getLogger(Jmcv.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public BufferedImage WebcamDetection() throws JmcvWebcamDetectionException {
        BufferedImage bufferedImage;
        try {
            if ((iplImage = grabber.grab()) != null) {
                cvClearMemStorage(storage);
                // In order to detect something a grayscale image is needed.
                cvCvtColor(iplImage, grayImage, CV_BGR2GRAY);
                CvSeq detectedObject = cvHaarDetectObjects(grayImage, classifier, storage,
                        1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
                int total = detectedObject.total();
                for (int i = 0; i < total; i++) {
                    cvRect = new CvRect(cvGetSeqElem(detectedObject, i));
                    int x = cvRect.x(), y = cvRect.y(), w = cvRect.width(), h = cvRect.height();
                    // reduce the rectangle
                    x += w / 4;
                    y += h / 4;
                    w = w / 2;
                    h = h / 2;
                    // draw a rectangle onto the face
                    cvRectangle(iplImage, cvPoint(x, y), cvPoint(x + w, y + h), CvScalar.MAGENTA, 1, CV_AA, 0);
                    detectionArea.setSize(iplImage.width(), iplImage.height());
                    detectionPoint.setSize(x + w / 2, y + h / 2);
                }
            }
            // dived image in three vertical sections
            int xLeft = iplImage.width()/3, xRight = 2*iplImage.width()/3;
            cvLine(iplImage, cvPoint(xLeft, 0), cvPoint(xLeft, iplImage.height()), CvScalar.WHITE);
            cvLine(iplImage, cvPoint(xRight, 0), cvPoint(xRight, iplImage.height()), CvScalar.WHITE);
            bufferedImage = iplImage.getBufferedImage();
            // flip the image to make background behave like a mirror
            // to players face
            AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
            tx.translate(-bufferedImage.getWidth(null), 0);
            AffineTransformOp op = new AffineTransformOp(tx,
                    AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            bufferedImage = op.filter(bufferedImage, null);
            return bufferedImage;
        } catch (FrameGrabber.Exception ex) {
            throw new JmcvWebcamDetectionException("Error capturing from webacam: " + ex);
        }
    }

    public boolean isDetectionOk() {
        return (cvRect != null);
    }

    public Dimension getDetectionPoint() {
        return detectionPoint;
    }

    public Dimension getDetectionArea() {
        return detectionArea;
    }
}
