import UIKit

extension UIImage {
    /// Downscales so the longest edge is at most `maxEdge` px (also normalising EXIF
    /// orientation) and encodes as JPEG. Phone photos are often 5–15 MB; labels stay
    /// perfectly legible at 2048 px and uploads get much faster.
    func preparedForUpload(maxEdge: CGFloat = 2048, quality: CGFloat = 0.85) -> (image: UIImage, jpeg: Data)? {
        let pixelSize = CGSize(width: size.width * scale, height: size.height * scale)
        let longest = max(pixelSize.width, pixelSize.height)
        let ratio = longest > maxEdge ? maxEdge / longest : 1
        let target = CGSize(width: (pixelSize.width * ratio).rounded(), height: (pixelSize.height * ratio).rounded())

        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        format.opaque = true
        let resized = UIGraphicsImageRenderer(size: target, format: format).image { _ in
            draw(in: CGRect(origin: .zero, size: target))
        }
        guard let jpeg = resized.jpegData(compressionQuality: quality) else { return nil }
        return (resized, jpeg)
    }
}
