plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1.2"

stonecutter parameters {
    constants.put("experimental", node.metadata.project.endsWith("-experimental"))
    replacements {
        string(current.parsed < "1.21.11") {
            replace("Identifier", "ResourceLocation")
        }
        string(current.parsed < "26.1") {
            replace("GuiGraphicsExtractor", "GuiGraphics")
            replace("graphics.text(", "graphics.drawString(")
        }
    }
}
