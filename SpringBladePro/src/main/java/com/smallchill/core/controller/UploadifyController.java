package com.smallchill.core.controller;

import java.io.File;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.smallchill.core.base.controller.BladeController;
import com.smallchill.core.constant.Cst;
import com.smallchill.core.plugins.dao.Db;
import com.smallchill.core.toolbox.Func;
import com.smallchill.core.toolbox.Record;
import com.smallchill.core.toolbox.file.BladeFile;
import com.smallchill.core.toolbox.file.UploadFileUtils;
import com.smallchill.core.toolbox.kit.PathKit;

@Controller
@RequestMapping("/uploadify")
public class UploadifyController extends BladeController {
	
	@ResponseBody
	@RequestMapping("/upload")
	public Record upload(@RequestParam("imgFile") MultipartFile file) {
		Record maps = Record.create();
		if (null == file) {
			maps.set("error", 1);
			maps.set("message", "请选择要上传的图片");
			return maps;
		}
		String originalFileName = file.getOriginalFilename();
		String dir = getPara("dir", "image");
		// 测试后缀
		boolean ok = UploadFileUtils.testExt(dir, originalFileName);
		if (!ok) {
			maps.set("error", 1);
			maps.set("message", "上传文件的类型不允许");
			return maps;
		}
		BladeFile bf = getFile(file);
		bf.transfer();
		Object fileId = bf.getFileId();	
		String url = "/uploadify/renderFile/" + fileId;
		maps.set("error", 0);
		maps.set("fileId", fileId);
		maps.set("url", Cst.me().getContextPath() + url);
		maps.set("fileName", originalFileName);
		return maps;	
	}
	
	@RequestMapping("/renderFile/{id}")
	public void renderFile(HttpServletResponse response, @PathVariable String id) {
		Map<String, Object> file = Db.init().findById("TFW_ATTACH", id);
		if (Func.isEmpty(file)) {
			redirect(response, "/error/error404");
			return;
		} else {
			String url = file.get("URL").toString();
			File f = new File((Cst.me().isRemoteMode() ? "" : PathKit.getWebRootPath()) + url);
			renderFile(response, f);
		}
	}
	
}
