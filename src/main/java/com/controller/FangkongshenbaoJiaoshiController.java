
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 教师防控申报
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/fangkongshenbaoJiaoshi")
public class FangkongshenbaoJiaoshiController {
    private static final Logger logger = LoggerFactory.getLogger(FangkongshenbaoJiaoshiController.class);

    private static final String TABLE_NAME = "fangkongshenbaoJiaoshi";

    @Autowired
    private FangkongshenbaoJiaoshiService fangkongshenbaoJiaoshiService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;

    //级联表非注册的service
    //注册表service
    @Autowired
    private YonghuService yonghuService;
    @Autowired
    private JiaoshiService jiaoshiService;
    @Autowired
    private XuexiaoService xuexiaoService;
    @Autowired
    private XueyuanService xueyuanService;


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        else if("教师".equals(role))
            params.put("jiaoshiId",request.getSession().getAttribute("userId"));
        else if("学校管理员".equals(role))
            params.put("xuexiaoId",request.getSession().getAttribute("userId"));
        else if("学院管理员".equals(role))
            params.put("xuanyuanTypes",xueyuanService.selectById((Integer) request.getSession().getAttribute("userId")).getXuanyuanTypes());
        CommonUtil.checkMap(params);
        PageUtils page = fangkongshenbaoJiaoshiService.queryPage(params);

        //字典表数据转换
        List<FangkongshenbaoJiaoshiView> list =(List<FangkongshenbaoJiaoshiView>)page.getList();
        for(FangkongshenbaoJiaoshiView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        FangkongshenbaoJiaoshiEntity fangkongshenbaoJiaoshi = fangkongshenbaoJiaoshiService.selectById(id);
        if(fangkongshenbaoJiaoshi !=null){
            //entity转view
            FangkongshenbaoJiaoshiView view = new FangkongshenbaoJiaoshiView();
            BeanUtils.copyProperties( fangkongshenbaoJiaoshi , view );//把实体数据重构到view中
            //级联表 教师
            //级联表
            JiaoshiEntity jiaoshi = jiaoshiService.selectById(fangkongshenbaoJiaoshi.getJiaoshiId());
            if(jiaoshi != null){
            BeanUtils.copyProperties( jiaoshi , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "jiaoshiId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setJiaoshiId(jiaoshi.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody FangkongshenbaoJiaoshiEntity fangkongshenbaoJiaoshi, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,fangkongshenbaoJiaoshi:{}",this.getClass().getName(),fangkongshenbaoJiaoshi.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("教师".equals(role))
            fangkongshenbaoJiaoshi.setJiaoshiId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        fangkongshenbaoJiaoshi.setInsertTime(new Date());
        Wrapper<FangkongshenbaoJiaoshiEntity> queryWrapper = new EntityWrapper<FangkongshenbaoJiaoshiEntity>()
                .eq("insert_time", new SimpleDateFormat("yyyy-MM-dd").format(fangkongshenbaoJiaoshi.getInsertTime()))
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        FangkongshenbaoJiaoshiEntity fangkongshenbaoJiaoshiEntity = fangkongshenbaoJiaoshiService.selectOne(queryWrapper);
        if(fangkongshenbaoJiaoshiEntity==null){
            fangkongshenbaoJiaoshi.setCreateTime(new Date());
            fangkongshenbaoJiaoshiService.insert(fangkongshenbaoJiaoshi);
            return R.ok();
        }else {
            return R.error(511,"一天一条申报信息，请不要重复添加");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody FangkongshenbaoJiaoshiEntity fangkongshenbaoJiaoshi, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,fangkongshenbaoJiaoshi:{}",this.getClass().getName(),fangkongshenbaoJiaoshi.toString());
        FangkongshenbaoJiaoshiEntity oldFangkongshenbaoJiaoshiEntity = fangkongshenbaoJiaoshiService.selectById(fangkongshenbaoJiaoshi.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("教师".equals(role))
//            fangkongshenbaoJiaoshi.setJiaoshiId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        //根据字段查询是否有相同数据
        Wrapper<FangkongshenbaoJiaoshiEntity> queryWrapper = new EntityWrapper<FangkongshenbaoJiaoshiEntity>()
            .notIn("id",fangkongshenbaoJiaoshi.getId())
            .andNew()
            .eq("jiaoshi_id", fangkongshenbaoJiaoshi.getJiaoshiId())
            .eq("keshou_types", fangkongshenbaoJiaoshi.getKeshouTypes())
            .eq("wuaichu_types", fangkongshenbaoJiaoshi.getWuaichuTypes())
            .eq("fangkongshenbao_jiaoshi_didian", fangkongshenbaoJiaoshi.getFangkongshenbaoJiaoshiDidian())
            .eq("binghuan_types", fangkongshenbaoJiaoshi.getBinghuanTypes())
            .eq("gaofengxian_types", fangkongshenbaoJiaoshi.getGaofengxianTypes())
            .eq("insert_time", new SimpleDateFormat("yyyy-MM-dd").format(fangkongshenbaoJiaoshi.getInsertTime()))
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        FangkongshenbaoJiaoshiEntity fangkongshenbaoJiaoshiEntity = fangkongshenbaoJiaoshiService.selectOne(queryWrapper);
        if(fangkongshenbaoJiaoshiEntity==null){
            fangkongshenbaoJiaoshiService.updateById(fangkongshenbaoJiaoshi);//根据id更新
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<FangkongshenbaoJiaoshiEntity> oldFangkongshenbaoJiaoshiList =fangkongshenbaoJiaoshiService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        fangkongshenbaoJiaoshiService.deleteBatchIds(Arrays.asList(ids));

        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            List<FangkongshenbaoJiaoshiEntity> fangkongshenbaoJiaoshiList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            FangkongshenbaoJiaoshiEntity fangkongshenbaoJiaoshiEntity = new FangkongshenbaoJiaoshiEntity();
//                            fangkongshenbaoJiaoshiEntity.setJiaoshiId(Integer.valueOf(data.get(0)));   //教师 要改的
//                            fangkongshenbaoJiaoshiEntity.setFangkongshenbaoJiaoshiTiwen(data.get(0));                    //当天体温 要改的
//                            fangkongshenbaoJiaoshiEntity.setKeshouTypes(Integer.valueOf(data.get(0)));   //有无咳嗽 要改的
//                            fangkongshenbaoJiaoshiEntity.setWuaichuTypes(Integer.valueOf(data.get(0)));   //有无外出 要改的
//                            fangkongshenbaoJiaoshiEntity.setFangkongshenbaoJiaoshiDidian(data.get(0));                    //外出地点 要改的
//                            fangkongshenbaoJiaoshiEntity.setBinghuanTypes(Integer.valueOf(data.get(0)));   //是否接触病患 要改的
//                            fangkongshenbaoJiaoshiEntity.setGaofengxianTypes(Integer.valueOf(data.get(0)));   //是否出入高风险区域 要改的
//                            fangkongshenbaoJiaoshiEntity.setFangkongshenbaoJiaoshiText(data.get(0));                    //备注 要改的
//                            fangkongshenbaoJiaoshiEntity.setInsertTime(date);//时间
//                            fangkongshenbaoJiaoshiEntity.setCreateTime(date);//时间
                            fangkongshenbaoJiaoshiList.add(fangkongshenbaoJiaoshiEntity);


                            //把要查询是否重复的字段放入map中
                        }

                        //查询是否重复
                        fangkongshenbaoJiaoshiService.insertBatch(fangkongshenbaoJiaoshiList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }





}
