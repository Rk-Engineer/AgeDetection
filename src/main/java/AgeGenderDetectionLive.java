import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.highgui.HighGui;

public class AgeGenderDetectionLive {

    private static final String[] AGE_GROUPS = new String[]{
            "(0-2)", "(4-6)", "(8-12)", "(15-20)", "(25-32)", "(38-43)", "(48-53)", "(60-100)"
    };

    private static final String[] GENDER_CLASSES = new String[]{
            "Male", "Female"
    };

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        VideoCapture camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            System.out.println("Error: Could not open the camera!");
            return;
        }

        CascadeClassifier faceDetector = new CascadeClassifier("K:\\Training\\AgeDetection\\haarcascade_frontalface_default.xml");
        String ageModelProto = "K:\\Training\\AgeDetection\\src\\main\\resources\\models\\age_deploy.prototxt";
        String ageModelWeights = "K:\\Training\\AgeDetection\\src\\main\\resources\\models\\age_net.caffemodel";
        Net ageNet = Dnn.readNetFromCaffe(ageModelProto, ageModelWeights);

        String genderModelProto = "K:\\Training\\AgeDetection\\src\\main\\resources\\models\\gender_deploy.prototxt";
        String genderModelWeights = "K:\\Training\\AgeDetection\\src\\main\\resources\\models\\gender_net.caffemodel";
        Net genderNet = Dnn.readNetFromCaffe(genderModelProto, genderModelWeights);

        Mat frame = new Mat();
        while (camera.read(frame)) {
            Mat grayFrame = new Mat();
            Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
            MatOfRect faceDetections = new MatOfRect();
            faceDetector.detectMultiScale(grayFrame, faceDetections, 1.1, 3, 0, new Size(30, 30), new Size());

            for (Rect rect : faceDetections.toArray()) {
                System.out.println("Detected face size: " + rect.width + "x" + rect.height);
                Imgproc.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0));

                // Extract the face ROI
                Mat faceROI = new Mat(frame, rect);

                // Resize if necessary
                Mat resizedFace = new Mat();
                if (faceROI.cols() < 227 || faceROI.rows() < 227) {
                    System.out.println("Detected face is too small, resizing.");
                    Imgproc.resize(faceROI, resizedFace, new Size(227, 227));
                } else {
                    resizedFace = faceROI;
                }

                // Create blobs from the resized face
                Mat ageBlob = Dnn.blobFromImage(resizedFace, 1.0, new Size(227, 227), new Scalar(104, 117, 123), false, false);
                Mat genderBlob = Dnn.blobFromImage(resizedFace, 1.0, new Size(227, 227), new Scalar(104, 117, 123), false, false);

                // Predict age
                ageNet.setInput(ageBlob);
                Mat agePredictions = ageNet.forward();
                Core.MinMaxLocResult ageResult = Core.minMaxLoc(agePredictions);
                int ageIndex = (int) ageResult.maxLoc.x;
                String predictedAge = AGE_GROUPS[ageIndex];

                // Predict gender
                genderNet.setInput(genderBlob);
                Mat genderPredictions = genderNet.forward();
                Core.MinMaxLocResult genderResult = Core.minMaxLoc(genderPredictions);
                int genderIndex = (int) genderResult.maxLoc.x;
                String predictedGender = GENDER_CLASSES[genderIndex];

                // Add the predicted age and gender to the frame
                String label = predictedGender + ", " + predictedAge;
                Imgproc.putText(frame, label, new Point(rect.x, rect.y - 10), Imgproc.FONT_HERSHEY_SIMPLEX, 0.9, new Scalar(255, 0, 0), 2);
            }

            // Display the frame
            HighGui.imshow("Age and Gender Detection - Live Camera", frame);

            // Break the loop if 'q' is pressed
            if (HighGui.waitKey(30) == 'q') {
                break;
            }
        }

        camera.release();
        HighGui.destroyAllWindows();
    }
}
