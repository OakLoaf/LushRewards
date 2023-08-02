dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.2")
    shadow(files("../libs/EnchantedStorage.jar"))
    shadow("com.github.CoolDCB:ChatColorHandler:v1.3.3")
}