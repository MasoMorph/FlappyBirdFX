module com.fbo.flappybirdfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.media;

    exports com.fbo;
    opens com.fbo to javafx.fxml;

    provides javafx.application.Application with com.fbo.FlappyBirdFX;
}
