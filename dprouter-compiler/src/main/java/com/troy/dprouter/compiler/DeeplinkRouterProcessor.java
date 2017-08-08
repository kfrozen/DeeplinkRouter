package com.troy.dprouter.compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.troy.dprouter.annotation.ActivityRouter;
import com.troy.dprouter.annotation.AllModules;
import com.troy.dprouter.annotation.FragmentRouter;
import com.troy.dprouter.annotation.Module;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

@AutoService(Processor.class)
public class DeeplinkRouterProcessor extends AbstractProcessor
{
    private static final String METHOD_NAME_INIT = "init";
    private static final String BASE_MODULE_MAPPING_CLASS_NAME = "RouterMapping";
    private static final String ROUTER_INIT_CLASS_NAME = "RouterInit";
    private static final String ROUTER_INIT_INTERFACE_NAME = "IDPRouterInit";
    private static final String ROUTER_API_PACKAGE_NAME = "com.troy.dprouter.api";

    private Messager mMessager;
    private Filer mFiler;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);

        mMessager = processingEnv.getMessager();

        mFiler = processingEnv.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes()
    {
        Set<String> annotationSet = new LinkedHashSet<>();

        annotationSet.add(ActivityRouter.class.getCanonicalName());
        annotationSet.add(FragmentRouter.class.getCanonicalName());

        return annotationSet;
    }

    @Override
    public SourceVersion getSupportedSourceVersion()
    {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        if (annotations.isEmpty())
        {
            return false;
        }

        boolean hasAllModulesAnnotation = false;
        boolean hasModuleAnnotation = false;

        //******** Parse AllModules annotation *************//
        String[] moduleNames = null;

        Set<? extends Element> allModulesElements = roundEnv.getElementsAnnotatedWith(AllModules.class);

        if(allModulesElements != null && !allModulesElements.isEmpty())
        {
            AllModules allModules = allModulesElements.iterator().next().getAnnotation(AllModules.class);

            moduleNames = allModules.moduleNames();

            hasAllModulesAnnotation = true;
        }
        //******** Parse AllModules annotation ends*************//

        //******** Parse Module annotation *************//
        String genClassName = BASE_MODULE_MAPPING_CLASS_NAME;

        Set<? extends Element> moduleElements = roundEnv.getElementsAnnotatedWith(Module.class);

        if(moduleElements != null && !moduleElements.isEmpty())
        {
            Module module = moduleElements.iterator().next().getAnnotation(Module.class);

            genClassName = genClassName + "_" + module.name();

            hasModuleAnnotation = true;
        }
        //******** Parse Module annotation ends*************//

        if(hasAllModulesAnnotation) //Main module, defines all the other modules, generate a main init class that will invoke all the module mapping class
        {
            generateRouterInitWithModules(moduleNames);
        }
        else if(!hasModuleAnnotation) //The project only has a single module, generate a default init class
        {
            generateRouterInitByDefault();
        }

        //This is a child module, so just generate its own mapping class, no init class needed here
        return generateModuleMapping(genClassName, roundEnv);
    }

    private void generateRouterInitWithModules(String[] moduleNames)
    {
        MethodSpec.Builder initMethodBuilder = MethodSpec.methodBuilder(METHOD_NAME_INIT)
                .addModifiers(Modifier.PUBLIC).addAnnotation(Override.class);

        for(String name : moduleNames)
        {
            if(name == null || "".equalsIgnoreCase(name))
            {
                continue;
            }

            initMethodBuilder.addStatement(BASE_MODULE_MAPPING_CLASS_NAME + "_" + name + ".init()");
        }

        innerGenerateRouterInit(initMethodBuilder);
    }

    private void generateRouterInitByDefault()
    {
        MethodSpec.Builder initMethodBuilder = MethodSpec.methodBuilder(METHOD_NAME_INIT)
                .addModifiers(Modifier.PUBLIC).addAnnotation(Override.class);

        initMethodBuilder.addStatement(BASE_MODULE_MAPPING_CLASS_NAME + ".init()");

        innerGenerateRouterInit(initMethodBuilder);
    }

    private void innerGenerateRouterInit(MethodSpec.Builder methodBuilder)
    {
        TypeSpec routerInit = TypeSpec.classBuilder(ROUTER_INIT_CLASS_NAME)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(ClassName.get(ROUTER_API_PACKAGE_NAME, ROUTER_INIT_INTERFACE_NAME))
                .addMethod(methodBuilder.build())
                .build();
        try
        {
            JavaFile.builder(ROUTER_API_PACKAGE_NAME, routerInit)
                    .build()
                    .writeTo(mFiler);
        }
        catch (Exception e)
        {
            error("Failed to generate file %s", routerInit.name);
        }
    }

    private boolean generateModuleMapping(String genClassName, RoundEnvironment roundEnv)
    {
        MethodSpec.Builder moduleInitBuilder = MethodSpec.methodBuilder(METHOD_NAME_INIT)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        moduleInitBuilder.addCode("\n");

        try
        {
            processRouters(ActivityRouter.class, roundEnv.getElementsAnnotatedWith(ActivityRouter.class), moduleInitBuilder);

            processRouters(FragmentRouter.class, roundEnv.getElementsAnnotatedWith(FragmentRouter.class), moduleInitBuilder);
        }
        catch (IllegalArgumentException e)
        {
            error(e.getMessage());

            return true;
        }

        TypeSpec moduleInit = TypeSpec.classBuilder(genClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(moduleInitBuilder.build())
                .build();
        try
        {
            JavaFile.builder(ROUTER_API_PACKAGE_NAME, moduleInit)
                    .build()
                    .writeTo(mFiler);
        }
        catch (Exception e)
        {
            error("Failed to generate file %s", moduleInit.name);

            return true;
        }

        return true;
    }

    private MethodSpec.Builder processRouters(Class<? extends Annotation> routerClass, Set<? extends Element> elements, MethodSpec.Builder methodBuilder)
    {
        for (Element element : elements)
        {
            if (element.getKind() != ElementKind.CLASS)
            {
                throw new IllegalArgumentException(String.format("%s can only be annotated to classes", routerClass.getSimpleName()));
            }

            ClassName className = ClassName.get((TypeElement) element);
            String paramFilterName = className.simpleName() + "ParamFilter";
            String[] paramArray;
            boolean hasParams;

            if(FragmentRouter.class.equals(routerClass))
            {
                FragmentRouter router = element.getAnnotation(FragmentRouter.class);

                paramArray = router.params();
                hasParams = paramArray.length > 0 && !"".equals(paramArray[0]);
            }
            else if(ActivityRouter.class.equals(routerClass))
            {
                ActivityRouter router = element.getAnnotation(ActivityRouter.class);

                paramArray = router.params();
                hasParams = paramArray.length > 0 && !"".equals(paramArray[0]);
            }
            else
            {
                throw new IllegalArgumentException("Router type has to be one of the defined annotation types");
            }

            if (hasParams)
            {
                methodBuilder.addStatement("android.support.v4.util.ArrayMap<String, String> $N = new android.support.v4.util.ArrayMap<>()", paramFilterName);

                for (String param : paramArray)
                {
                    String[] keyValuePair = param.split("=");

                    if(keyValuePair.length != 2) continue;

                    methodBuilder.addStatement("$N.put($S, $S)", paramFilterName, keyValuePair[0], keyValuePair[1]);
                }
            }

            if(FragmentRouter.class.equals(routerClass))
            {
                FragmentRouter router = element.getAnnotation(FragmentRouter.class);

                for (String host : router.hosts())
                {
                    if (host.startsWith("/"))
                    {
                        throw new IllegalArgumentException("Router host cannot start with '/'");
                    }

                    if (host.endsWith("/"))
                    {
                        throw new IllegalArgumentException("Router host cannot end with '/'");
                    }

                    if (hasParams)
                    {
                        methodBuilder.addStatement("com.troy.dprouter.api.DPRouter.mapFragment($S, $T.class, $N)", host, className, paramFilterName);
                    }
                    else
                    {
                        methodBuilder.addStatement("com.troy.dprouter.api.DPRouter.mapFragment($S, $T.class, null)", host, className);
                    }

                    methodBuilder.addCode("\n");
                }
            }
            else if(ActivityRouter.class.equals(routerClass))
            {
                ActivityRouter router = element.getAnnotation(ActivityRouter.class);

                for (String host : router.hosts())
                {
                    if (host.startsWith("/"))
                    {
                        throw new IllegalArgumentException("Router host cannot start with '/'");
                    }

                    if (host.endsWith("/"))
                    {
                        throw new IllegalArgumentException("Router host cannot end with '/'");
                    }

                    if (hasParams)
                    {
                        methodBuilder.addStatement("com.troy.dprouter.api.DPRouter.mapActivity($S, $T.class, $N)", host, className, paramFilterName);
                    }
                    else
                    {
                        methodBuilder.addStatement("com.troy.dprouter.api.DPRouter.mapActivity($S, $T.class, null)", host, className);
                    }

                    methodBuilder.addCode("\n");
                }
            }
        }

        return methodBuilder;
    }

    private void error(String msg, Object... args)
    {
        mMessager.printMessage(Kind.ERROR, args == null ? msg : String.format(msg, args));
    }

}
