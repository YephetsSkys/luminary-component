/**  
* <p>Title: AopTracker.java</p>  
* <p>Description: </p>  
* <p>Copyright: Copyright (c) 2018-2099</p>  
* <p>Company: </p>  
* @author wulinfeng  
* @date 2018年7月24日上午11:41:23  
*/  
package com.luminary.component.feign.tracker;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.gson.Gson;
import com.luminary.component.feign.model.FeignTraceHolder;
import com.luminary.component.ribbon.rule.LuminaryRibbonRule;
import com.luminary.component.trace.annotation.Trace;
import com.luminary.component.trace.client.TraceClient;
import com.luminary.component.trace.model.RpcTraceInfoVO;
import com.luminary.component.trace.model.RpcTypeEnum;
import com.luminary.component.trace.model.TraceInfo;
import com.luminary.component.trace.thread.TraceContext;
import com.luminary.component.trace.tracker.GenericTracker;
import com.luminary.component.trace.tracker.GenericTracker.TraceHolder;
import com.luminary.component.trace.tracker.Tracker;
import com.luminary.component.util.web.HostUtil;

import lombok.extern.slf4j.Slf4j;

/**  
* <p>Title: AopTracker</p>  
* <p>Description: 基于feign的链路跟踪器</p>  
* @author wulinfeng
* @date 2018年7月24日上午11:41:23
*/
@Slf4j
@Aspect
@Component
public class FeignTracker extends GenericTracker implements Tracker<TraceHolder> {
	
	@Value("${spring.profiles.active:default}")
	private String profile;
	
	@Autowired
	private TraceClient traceClient;
	
	/**  
	* <p>Title: </p>  
	* <p>Description: </p>  
	* @param traceClient  
	*/  
	public FeignTracker(TraceClient traceClient) {
		super(traceClient);
		this.traceClient = traceClient;
	}

	@Around(value = "@annotation(trace)")
	public Object proceed(ProceedingJoinPoint joinPoint, Trace trace) throws Throwable   {
		
		if(!trace.value().getName().equals(this.getClass().getName())) {
			return joinPoint.proceed();
		}
		
		FeignTraceHolder traceHolder = new FeignTraceHolder();
		Object result = null;
		
		try {
			
			log.info("feign tracker");
			
			Gson gson = new Gson();
			
			Signature signature = joinPoint.getSignature();
		    MethodSignature methodSignature = (MethodSignature) signature;
		    Method method = methodSignature.getMethod();
		    String methodName = method.getName();

		    FeignClient feignClient = AnnotationUtils.findAnnotation(method.getDeclaringClass(), FeignClient.class);
		    
		    GetMapping getMapping = method.getAnnotation(GetMapping.class);
		    PostMapping postMapping = method.getAnnotation(PostMapping.class);
		    PutMapping putMapping = method.getAnnotation(PutMapping.class);
		    DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
		    RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);

		    String serverHost = "";
		    if(getMapping != null) {
		    	serverHost = StringUtils.join(getMapping.value(), "/");
		    }
		    else if(postMapping != null) {
		    	serverHost = StringUtils.join(postMapping.value(), "/");
		    }
		    else if(putMapping != null) {
		    	serverHost = StringUtils.join(putMapping.value(), "/");
		    }
		    else if(deleteMapping != null) {
		    	serverHost = StringUtils.join(deleteMapping.value(), "/");
		    }
		    else if(requestMapping != null) {
		    	serverHost = StringUtils.join(requestMapping.value(), "/");
		    }
		    
		    Map<String, Object> requestMap = new HashMap<String, Object>();
		    List<String> requestList = new ArrayList<String>();
		    
		    Object[] args = joinPoint.getArgs();
		    for(Object arg : args) {
		    	requestList.add(arg.toString());
		    }
		    
		    requestMap.put("params", requestList);
		    
		    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		    
		    traceHolder.setProfile(profile);
		    traceHolder.setRpcType(RpcTypeEnum.HTTP.name());
			traceHolder.setServiceCategory("feign");
			traceHolder.setServiceName(feignClient.name());
			traceHolder.setMethodName(methodName);
			traceHolder.setRequestParam(gson.toJson(requestMap));
			traceHolder.setServiceHost(serverHost);
			traceHolder.setClientHost(HostUtil.getIP(request));
			
			LuminaryRibbonRule.regist(traceHolder);
			
			preHandle(traceHolder);
			request.setAttribute(TraceInfo.TRACE_ID_KEY, traceHolder.getEntity().getTraceId());
			request.setAttribute(TraceInfo.RPC_ID_KEY, traceHolder.getEntity().getRpcId());
			result = joinPoint.proceed();
			traceHolder.getEntity().setResponseInfo(result.toString());
			postHandle(traceHolder);
		
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			exceptionHandle(traceHolder, e);
		}
			
		return result;
		
	}
	
}
