package com.example.demo.Service;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avutil;
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

    public static void main(String[] args)
    {
        //                String url = "/Users/magi_0/Desktop/Screen_Shot/01.mp4";
        String url       = "rtmp://localhost:1935/hls/123";
        String storePath = "/Users/magi_0/Desktop/工作/EasyAI/TestStore";

        MediaImageTransfer mediaImageTransfer = new MediaImageTransfer();
        mediaImageTransfer.rtspToImage(url, 30, storePath, 101L);
    }

    public void rtspToImage(
            String url, Integer grabRate, String storePath, Long id)
    {
        // 采集/抓取器
        FFmpegFrameGrabber grabbers = new FFmpegFrameGrabber(url);
        // 图片转换工具
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

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

        grabbers.setFrameRate(30);
        log.info("rate: " + grabbers.getFrameRate());

        // 设置默认长宽
        grabbers.setImageWidth(grabbers.getImageWidth());
        grabbers.setImageHeight(grabbers.getImageHeight());


        try
        {
            // 开始抓取
            grabbers.start();


            Frame grabFrames  = grabbers.grabImage();
            int   fileCounter = 0;
            while (grabFrames != null)
            {
                if (fileCounter % grabRate == 0)
                {
                    /* 转换图片 */
                    Mat mat = converter.convertToMat(grabFrames);

                    StringBuffer sb = new StringBuffer(storePath + "/" + id + "_" + (fileCounter++) + ".jpg");
                    /* 本地存图 */
                    opencv_imgcodecs.imwrite(sb.toString(), mat);
                }
                /* 抓取下一帧 */
                grabFrames = grabbers.grabImage();

            }
        } catch (Exception e)
        {
            log.error("Converter error: " + e);
        } finally
        {
            // 关闭抓取
            close(grabbers);
            log.info("Grabber closed.");
        }
    }

    public void close(FFmpegFrameGrabber grabbers)
    {
        try
        {
            grabbers.close();
        } catch (FrameGrabber.Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
