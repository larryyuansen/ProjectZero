package com.example.demo.Service;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.io.OutputStream;

/**
 * 转换rtsp为image
 *
 * @author IT_CREATE
 * {@code @date} 2021/6/8 12:00:00
 */
@Slf4j
@Service
public class MediaImageTransfer
{

    public static void rtspToImage(
            String url, Integer grabRate, OutputStream outStream, String storePath, int id) throws Exception
    {
        // 采集/抓取器
        FFmpegFrameGrabber grabbers = new FFmpegFrameGrabber(url);
        // 图片转换工具
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

        // 传输格式
        grabbers.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        grabbers.setOption("buffer_size", "1024000");
        // socket网络超时时间
        // 设置打开协议tcp / udp
        grabbers.setOption("rtsp_transport", "tcp");
        // 首选TCP进行RTP传输
        grabbers.setOption("rtsp_flags", "prefer_tcp");
        // 设置超时时间
        // -stimeout 的单位是us 微秒(1秒=1*1000*1000微秒)。
        grabbers.setOption("stimeout", "5*1000*1000");

        // 设置默认长宽
        grabbers.setImageWidth(grabbers.getImageWidth());
        grabbers.setImageHeight(grabbers.getImageHeight());

        /* logger */
        log.info(String.format(
                "stream: " + grabbers.getImageHeight() + ":" + grabbers.getImageWidth() + ":" + grabbers.getFormat()));

        // 开始抓取
        grabbers.start();
        Frame grabFrames = grabbers.grab();

        // 监视器 (监视抓取)
        CanvasFrame frames = new CanvasFrame("camera", CanvasFrame.getDefaultGamma() / grabbers.getGamma());
        frames.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frames.setAlwaysOnTop(false);

        int fileCounter = 0;
        while (grabFrames != null)
        {
            /* 转换图片 */
            IplImage grabbedImages = converter.convert(grabFrames);

            /* 监视器展示 */
            frames.showImage(grabFrames);

            StringBuffer sb = new StringBuffer(storePath + "/" + id + "_" + (fileCounter++) + ".jpg");
            /* 本地存图 */
            opencv_imgcodecs.cvSaveImage(sb.toString(), grabbedImages);

            /* 等待 */
            //            Thread.sleep(1000);

            /* 抓取下一帧 */
            if (Integer.valueOf(0).equals(grabRate))
            {
                grabFrames = grabbers.grabKeyFrame();
            } else
            {
                grabFrames = grabbers.grabAtFrameRate();
            }
        }

        // 关闭抓取
        grabbers.close();
        log.info("Grabber closed.");
    }

    public static void main(String[] args)
    {
        //        String url       = "/Users/magi_0/Desktop/Screen_Shot/01.mp4";
        String url       = "rtmp://localhost:1935/hls/123";
        String storePath = "/Users/magi_0/Desktop/工作/EasyAI/TestStore";

        try
        {
            rtspToImage(url, 0, null, storePath, 101);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
