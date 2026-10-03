interface CompressOptions {
  /** Longest side in pixels after resizing. */
  maxSide: number
  /** PNG keeps transparency (logos); JPEG is much smaller for photos. */
  type: 'image/jpeg' | 'image/png'
  quality?: number
}

/**
 * Resizes and re-encodes an image in the browser before uploading it. The free Supabase Storage
 * has 1 GB, a phone photo is 3-6 MB and the courts' connection is poor, so we upload ~300 KB.
 * Re-encoding through a canvas also drops the EXIF metadata, including the GPS position.
 */
export async function compressImage(file: Blob, { maxSide, type, quality = 0.82 }: CompressOptions): Promise<Blob> {
  const image = await loadImage(file)
  const scale = Math.min(1, maxSide / Math.max(image.naturalWidth, image.naturalHeight))
  const canvas = document.createElement('canvas')
  canvas.width = Math.round(image.naturalWidth * scale)
  canvas.height = Math.round(image.naturalHeight * scale)
  const context = canvas.getContext('2d')
  if (!context) throw new Error('Canvas 2D not available')
  if (type === 'image/jpeg') {
    // JPEG has no transparency: paint white instead of black behind transparent areas.
    context.fillStyle = '#ffffff'
    context.fillRect(0, 0, canvas.width, canvas.height)
  }
  context.drawImage(image, 0, 0, canvas.width, canvas.height)
  return new Promise((resolve, reject) =>
    canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('Could not encode image'))), type, quality),
  )
}

/** Through an <img>, which applies the photo's EXIF orientation in every current browser. */
function loadImage(file: Blob): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const image = new Image()
    image.onload = () => {
      URL.revokeObjectURL(url)
      resolve(image)
    }
    image.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('Not an image'))
    }
    image.src = url
  })
}
