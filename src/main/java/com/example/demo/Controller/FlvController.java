package com.example.demo.Controller;

import com.example.demo.Service.AjaxResult;
import com.example.demo.Service.MediaVideoTransfer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * MainController
 *
 * @author zys
 * {@code @date} 2022/08/12
 */
@Slf4j
@Controller
public class FlvController
{
    AtomicInteger                                 sign            = new AtomicInteger();
    ConcurrentHashMap<Integer, String>            pathMap         = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, PipedOutputStream> outputStreamMap = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, PipedInputStream>  inputStreamMap  = new ConcurrentHashMap<>();

    public static void main(String[] args) throws FileNotFoundException
    {
        FlvController indexController = new FlvController();
        AjaxResult    ajaxResult      = indexController.putVideoPath("/Users/magi_0/Desktop/Screen_Shot/00.mov");
        indexController.write((int) ajaxResult.get("data"),
                              new FileOutputStream("/Users/magi_0/Desktop/Screen_Shot/00.flv"));
    }

    @GetMapping("/")
    public String indexView()
    {
        return "index";
    }

    @GetMapping("/test")
    public String testView()
    {
        return "test";
    }

    @PostMapping("/putVideo")
    @ResponseBody
    public AjaxResult putVideoPath(String path)
    {
        try
        {
            int id = sign.getAndIncrement();
            pathMap.put(id, path);
            PipedOutputStream pipedOutputStream = new PipedOutputStream();
            PipedInputStream  pipedInputStream  = new PipedInputStream();
            pipedOutputStream.connect(pipedInputStream);
            outputStreamMap.put(id, pipedOutputStream);
            inputStreamMap.put(id, pipedInputStream);
            return AjaxResult.success(id);
        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
            return AjaxResult.error();
        }
    }

    @CrossOrigin
    @GetMapping("/getVideo")
    public void getVideo(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(required = false, name = "id") Integer id,
            @RequestParam(required = false, name = "url") String url)
    {
        if (url == null || "".equals(url))
        {
            //            url = "/root/jar/mp4Test.mp4";
            //            url = "/Users/magi_0/Desktop/Screen_Shot/01.mp4";
            url = "rtmp://127.0.0.1:1935/hls/123";
        }
        id = (int) putVideoPath(url).get(AjaxResult.DATA_TAG);


        log.info("进来了" + id);
        String path     = pathMap.get(id);
        String fileName = UUID.randomUUID().toString();
        /* - 测试 - 用于测试的时候，本地文件读取走这里  - START*/
        if (path.endsWith(".mp4"))
        {
            String[] split = new File(path).getName().split("\\.");
            fileName = split[0];
        }
        /* - 测试 - END */
        response.addHeader("Content-Disposition", "attachment;filename=" + fileName + ".flv");
        try
        {
            ServletOutputStream outputStream = response.getOutputStream();
            write(id, outputStream);
        } catch (IOException e)
        {
            log.error(e.getMessage(), e);
        }
    }

    private void write(int id, OutputStream outputStream)
    {
        try
        {
            String            path              = pathMap.get(id);
            PipedOutputStream pipedOutputStream = outputStreamMap.get(id);

            new Thread(() -> {
                MediaVideoTransfer mediaVideoTransfer = new MediaVideoTransfer();
                mediaVideoTransfer.setOutputStream(outputStream);
                mediaVideoTransfer.setRtspTransportType("tcp");
                mediaVideoTransfer.setRtspUrl(path);
                mediaVideoTransfer.live();
            }).start();

            print(inputStreamMap.get(id), outputStream);
        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }

    private void print(InputStream inputStream, OutputStream outputStream) throws IOException
    {
        byte[] buffer = new byte[1024];
        int    length;
        while ((length = inputStream.read(buffer)) != -1)
        {
            outputStream.write(buffer, 0, length);
        }
    }
}

