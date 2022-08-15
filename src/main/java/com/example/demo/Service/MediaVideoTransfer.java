package com.example.demo.Service;


import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.IplImage;

import javax.swing.*;
import java.io.OutputStream;

/**
 * 转换rtsp为flv
 *
 * @author IT_CREATE
 * {@code @date} 2021/6/8 12:00:00
 */
@Slf4j
public class MediaVideoTransfer
{

    @Setter
    private OutputStream outputStream;

    @Setter
    private String rtspUrl;

    @Setter
    private String rtspTransportType;

    private FFmpegFrameGrabber grabber;

    private FFmpegFrameRecorder recorder;

    private boolean isStart = false;

    public static void rtspToImage(String url, Integer grabRate, OutputStream outStream) throws Exception
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
        grabbers.setImageWidth(Math.min(grabbers.getImageWidth(), 1280));
        grabbers.setImageHeight(Math.min(grabbers.getImageHeight(), 720));

        /* logger */
        log.info(String.format(
                "stream: " + grabbers.getImageHeight() + ":" + grabbers.getImageWidth() + ":" + grabbers.getFormat()));

        // 开始抓取
        grabbers.start();
        Frame grabFrames = grabbers.grab();

        // 监视器 (监视抓取)
        CanvasFrame frames = new CanvasFrame("camera", CanvasFrame.getDefaultGamma() / grabbers.getGamma());
        frames.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frames.setAlwaysOnTop(true);

        while (grabFrames != null)
        {
            /* 转换图片 */
            IplImage grabbedImages = converter.convert(grabFrames);

            /* 监视器展示 */
            frames.showImage(grabFrames);

            /* 本地存图 */
            /* opencv_imgcodecs.cvSaveImage("hello.jpg", grabbedImages); */
            
            /* 等待 */
            Thread.sleep(1000);

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

    /**
     * 开启获取rtsp流
     */
    public void live()
    {
        log.info("连接rtsp：" + rtspUrl + ",开始创建grabber");
        boolean isSuccess = createGrabber(rtspUrl);
        if (isSuccess)
        {
            log.info("创建grabber成功");
        } else
        {
            log.info("创建grabber失败");
        }
        startCameraPush();
    }

    /**
     * 构造视频抓取器
     *
     * @param rtsp 拉流地址
     * @return 创建成功与否
     */
    private boolean createGrabber(String rtsp)
    {
        // 获取视频源
        try
        {
            grabber = FFmpegFrameGrabber.createDefault(rtsp);
            grabber.setOption("rtsp_transport", rtspTransportType);
            grabber.start();
            isStart = true;

            recorder = new FFmpegFrameRecorder(outputStream, grabber.getImageWidth(), grabber.getImageHeight(),
                                               grabber.getAudioChannels());
            //avcodec.AV_CODEC_ID_H264  //AV_CODEC_ID_MPEG4
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFormat("flv");
            recorder.setFrameRate(grabber.getFrameRate());
            recorder.setSampleRate(grabber.getSampleRate());
            recorder.setAudioChannels(grabber.getAudioChannels());
            recorder.setFrameRate(grabber.getFrameRate());
            return true;
        } catch (FrameGrabber.Exception e)
        {
            log.error("创建解析rtsp FFmpegFrameGrabber 失败");
            log.error("create rtsp FFmpegFrameGrabber exception: ", e);
            stop();
            reset();
            return false;
        }
    }

    /**
     * 推送图片（摄像机直播）
     */
    private void startCameraPush()
    {
        if (grabber == null)
        {
            log.info("重试连接rtsp：" + rtspUrl + ",开始创建grabber");
            boolean isSuccess = createGrabber(rtspUrl);
            if (isSuccess)
            {
                log.info("创建grabber成功");
            } else
            {
                log.info("创建grabber失败");
            }
        }
        try
        {
            if (grabber != null)
            {
                recorder.start();
                Frame frame;
                while (isStart && (frame = grabber.grabFrame()) != null)
                {
                    recorder.setTimestamp(grabber.getTimestamp());
                    recorder.record(frame);
                }
                stop();
                reset();
            }
        } catch (FrameGrabber.Exception | RuntimeException | FrameRecorder.Exception e)
        {
            log.error(e.getMessage(), e);
            stop();
            reset();
        }
    }

    private void stop()
    {
        try
        {
            if (recorder != null)
            {
                recorder.stop();
                recorder.release();
            }
            if (grabber != null)
            {
                grabber.stop();
            }
        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }

    private void reset()
    {
        recorder = null;
        grabber  = null;
        isStart  = false;
    }
}

