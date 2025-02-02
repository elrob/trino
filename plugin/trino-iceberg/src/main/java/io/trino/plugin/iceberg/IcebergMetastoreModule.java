/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.plugin.iceberg;

import com.google.inject.Binder;
import com.google.inject.Module;
import io.airlift.configuration.AbstractConfigurationAwareModule;
import io.trino.plugin.hive.metastore.HiveMetastore;
import io.trino.plugin.hive.metastore.cache.CachingHiveMetastore;
import io.trino.plugin.hive.metastore.cache.CachingHiveMetastoreModule;
import io.trino.plugin.hive.metastore.cache.ForCachingHiveMetastore;
import io.trino.plugin.hive.metastore.file.FileMetastoreModule;
import io.trino.plugin.hive.metastore.thrift.ThriftMetastoreModule;

import javax.inject.Inject;

import java.util.Optional;

import static io.airlift.configuration.ConditionalModule.conditionalModule;
import static io.trino.plugin.iceberg.CatalogType.HIVE_METASTORE;
import static io.trino.plugin.iceberg.CatalogType.TESTING_FILE_METASTORE;
import static java.util.Objects.requireNonNull;

public class IcebergMetastoreModule
        extends AbstractConfigurationAwareModule
{
    private final Optional<HiveMetastore> metastore;

    public IcebergMetastoreModule(Optional<HiveMetastore> metastore)
    {
        this.metastore = requireNonNull(metastore, "metastore is null");
    }

    @Override
    protected void setup(Binder binder)
    {
        if (metastore.isPresent()) {
            binder.bind(HiveMetastore.class).annotatedWith(ForCachingHiveMetastore.class).toInstance(metastore.get());
            install(new CachingHiveMetastoreModule());
        }
        else {
            bindMetastoreModule(HIVE_METASTORE, new ThriftMetastoreModule());
            bindMetastoreModule(TESTING_FILE_METASTORE, new FileMetastoreModule());
            // TODO add support for Glue metastore
        }

        binder.bind(MetastoreValidator.class).asEagerSingleton();
    }

    public static class MetastoreValidator
    {
        @Inject
        public MetastoreValidator(HiveMetastore metastore)
        {
            if (metastore instanceof CachingHiveMetastore) {
                throw new RuntimeException("Hive metastore caching must not be enabled for Iceberg");
            }
        }
    }

    private void bindMetastoreModule(CatalogType catalogType, Module module)
    {
        install(conditionalModule(
                IcebergConfig.class,
                config -> config.getCatalogType() == catalogType,
                module));
    }
}
