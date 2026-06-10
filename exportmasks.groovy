import qupath.lib.images.servers.ImageServer
import qupath.lib.regions.RegionRequest
import javax.imageio.ImageIO
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File

// --- SETTINGS ---
double downsample = 1.0 
def outputDir = "Path to store images"
def outputMask = "Path to store masks"

def imageData = getCurrentImageData()
def server = imageData.getServer()
def name = GeneralTools.getNameWithoutExtension(server.getMetadata().getName())

// --- PROCESS ---

// 1. Export the Full Image
def pathImage = buildFilePath(outputDir, name + "_original.jpg")
// Optimization: Use RegionRequest to respect downsampling, preventing out-of-memory crashes
def request = RegionRequest.createInstance(server.getPath(), downsample, 0, 0, server.getWidth(), server.getHeight())
writeImageRegion(server, request, pathImage)
println "Full image exported to: " + pathImage

// 2. Export a Binary Mask for the "Cortex" Annotation
def annotations = getAnnotationObjects()

// Fix: Correct way to look for a QuPath classification name
def cortexAnnotation = annotations.find { it.getPathClass()?.getName() == "Cortex" }

if (cortexAnnotation != null) {
    int width = (int) Math.ceil(server.getWidth() / downsample)
    int height = (int) Math.ceil(server.getHeight() / downsample)
    
    // Create a blank binary image (black background)
    def mask = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY)
    def g2d = mask.createGraphics()
    
    // Scale for downsampling if necessary
    g2d.scale(1.0/downsample, 1.0/downsample)
    
    // Fix: Use the found cortexAnnotation variable safely
    g2d.setColor(Color.WHITE)
    def shape = cortexAnnotation.getROI().getShape()
    g2d.fill(shape)
    g2d.dispose()
    
    // Save mask with a unique name
    def maskName = String.format("%s_mask.png", name)
    def pathMask = buildFilePath(outputMask, maskName)
    ImageIO.write(mask, "png", new File(pathMask))
    println "Exported mask: " + maskName
} else {
    println "Skipped: 'Cortex' annotation not present."
}

