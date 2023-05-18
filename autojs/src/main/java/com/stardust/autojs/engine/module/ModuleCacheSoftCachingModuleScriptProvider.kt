package com.stardust.autojs.engine.module

import org.mozilla.javascript.commonjs.module.ModuleScript
import org.mozilla.javascript.commonjs.module.provider.ModuleSourceProvider
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider

class ModuleCacheSoftCachingModuleScriptProvider(moduleSourceProvider: ModuleSourceProvider) :
    SoftCachingModuleScriptProvider(
        moduleSourceProvider
    ) {

    override fun putLoadedModule(moduleId: String, moduleScript: ModuleScript, validator: Any?) {
        super.putLoadedModule(moduleId, moduleScript, validator)
    }

    override fun getLoadedModule(moduleId: String?): CachedModuleScript {
        return super.getLoadedModule(moduleId)
    }
}