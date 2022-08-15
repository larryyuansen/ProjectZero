package com.example.demo.Service;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.stereotype.Service;

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

    boolean running = false;

    public static void main(String[] args)
    {
        //        String url = "/Users/magi_0/Desktop/Screen_Shot/01.mp4";
        //        String url = "rtmp://localhost:1935/hls/123";
        String url = "http://120.77.159.92:19021/getVideo";

        String storePath = "/Users/magi_0/Desktop/工作/EasyAI/TestStore";

        try
        {
            MediaImageTransfer mediaImageTransfer = new MediaImageTransfer();
            mediaImageTransfer.rtspToImage(url, 1.0, storePath, 101L);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void rtspToImage(
            String url, Double grabRate, String storePath, Long id)
    {
        // 采集/抓取器
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(url);
        // 图片转换工具
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

        // 传输格式
        //        grabber.setPixelFormat(avutil.AV_PIX_FMT_BGR24);
        grabber.setOption("buffer_size", "1024000");
        // socket网络超时时间
        // 设置打开协议tcp / udp
        grabber.setOption("rtsp_transport", "tcp");
        // 首选TCP进行RTP传输
        grabber.setOption("rtsp_flags", "prefer_tcp");
        // 设置超时时间
        // -stimeout 的单位是us 微秒(1秒=1*1000*1000微秒)。
        grabber.setOption("stimeout", "5*1000*1000");

        // 设置默认长宽
        grabber.setImageWidth(grabber.getImageWidth());
        grabber.setImageHeight(grabber.getImageHeight());

        try
        {
            // 开始抓取
            grabber.start();
            running = true;


            int   fileCounter = 0;
            Frame grabFrames  = grabber.grabKeyFrame();
            while (grabFrames != null && running)
            {
                /* 转换图片 */
                Mat mat = converter.convertToMat(grabFrames);

                /* 本地存图 */
                StringBuffer sb = new StringBuffer(storePath + "/" + id + "_" + (fileCounter++) + ".jpg");
                opencv_imgcodecs.imwrite(sb.toString(), mat);

                /* 抓取下一帧 */
                grabFrames = grabber.grabKeyFrame();
            }
        } catch (FFmpegFrameGrabber.Exception e)
        {
            throw new RuntimeException(e);
        } finally
        {
            // 关闭抓取
            shutdown(grabber);
        }
    }

    public void shutdown(FFmpegFrameGrabber grabber)
    {
        try
        {
            grabber.close();
            log.info("Grabber closed.");
        } catch (FrameGrabber.Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
