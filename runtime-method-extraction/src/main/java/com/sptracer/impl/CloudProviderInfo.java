package com.sptracer.impl;

import javax.annotation.Nullable;

@SuppressWarnings("StringBufferReplaceableByString")
public class CloudProviderInfo {

    private final String provider;

    @Nullable
    private String availabilityZone;

    @Nullable
    private String region;

    @Nullable
    private NameAndIdField instance;

    @Nullable
    private NameAndIdField account;

    @Nullable
    private NameAndIdField project;

    @Nullable
    private ProviderMachine machine;

    public CloudProviderInfo(String provider) {
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }

    @Nullable
    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(@Nullable String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    @Nullable
    public String getRegion() {
        return region;
    }

    public void setRegion(@Nullable String region) {
        this.region = region;
    }

    @Nullable
    public NameAndIdField getInstance() {
        return instance;
    }

    public void setInstance(@Nullable NameAndIdField instance) {
        this.instance = instance;
    }

    @Nullable
    public NameAndIdField getAccount() {
        return account;
    }

    public void setAccount(@Nullable NameAndIdField account) {
        this.account = account;
    }

    @Nullable
    public NameAndIdField getProject() {
        return project;
    }

    public void setProject(@Nullable NameAndIdField project) {
        this.project = project;
    }

    @Nullable
    public ProviderMachine getMachine() {
        return machine;
    }

    public void setMachine(@Nullable ProviderMachine machine) {
        this.machine = machine;
    }

    public static class NameAndIdField {
        @Nullable
        protected String id;
        @Nullable
        protected String name;

        public NameAndIdField(@Nullable String name) {
            this.name = name;
        }

        public NameAndIdField(@Nullable String name, @Nullable Long id) {
            this.name = name;
            this.id = id != null ? id.toString() : null;
        }

        public NameAndIdField(@Nullable String name, @Nullable String id) {
            this.name = name;
            this.id = id;
        }

        public boolean isEmpty() {
            return id == null && name == null;
        }

        @Nullable
        public String getId() {
            return id;
        }

        @Nullable
        public String getName() {
            return name;
        }

        public void setName(@Nullable String name) {
            this.name = name;
        }

        public void setId(@Nullable String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("{");
            sb.append("id='").append(id).append('\'');
            sb.append(", name='").append(name).append('\'');
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * Currently, we only fill the {@code account.id} field, however the intake API supports the {@code account.name}
     * as well and we may wont to use it in the future. This is a convenience type for the time being that can be
     * removed if we get to that.
     */
    public static class ProviderAccount extends NameAndIdField {

        public ProviderAccount(@Nullable String id) {
            super(null, id);
        }

        @Override
        public String toString() {
            return String.valueOf(id);
        }
    }

    public static class ProviderMachine {

        private final String type;

        public ProviderMachine(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudProviderInfo{");
        sb.append("provider='").append(provider).append('\'');
        sb.append(", availabilityZone='").append(availabilityZone).append('\'');
        sb.append(", region='").append(region).append('\'');
        sb.append(", instance=").append(instance);
        sb.append(", account=").append(account);
        sb.append(", project=").append(project);
        sb.append(", machine=").append(machine);
        sb.append('}');
        return sb.toString();
    }
}