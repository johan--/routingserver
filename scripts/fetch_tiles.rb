
require 'net/http'

(0..0).each do |x|
  (0..0).each do |y|

    tile_name = "#{x}_#{y}.png"

    Net::HTTP.start("access.nypl.org") { |http|
      resp = http.get("image.php/510d47e2-0961-a3d9-e040-e00a18064a99/tiles/0/13/#{tile_name}")
      puts resp
      open(tile_name ,"wb") { |file|
        file.write(resp.body)
      }
    }

  end
end
