local user_tokens_key = KEYS[1]
local timestamp_key = KEYS[2]

local fillrate = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

local fill_time = capacity/fillrate
local ttl = fill_time

local avail_tokens = tonumber(redis.call("get", user_tokens_key))
if avail_tokens == nil then
  avail_tokens = capacity
end

local bucket_last_refreshed = tonumber(redis.call("get", timestamp_key))
if bucket_last_refreshed == nil then
  bucket_last_refreshed = 0
end

local time_since_last_refresh = math.max(0, now-bucket_last_refreshed)
-- simulate continuous refill of the bucket at the fillrate. we take min, because bucket can't hold more than capacity
local tokens_after_refill = math.min(capacity, avail_tokens+(time_since_last_refresh*fillrate))  -- filling the bucket at the fillrate
local allowed = tokens_after_refill >= requested
local new_tokens = tokens_after_refill
if allowed then
  new_tokens = tokens_after_refill - requested
end

redis.call("setex", user_tokens_key, ttl, new_tokens)  --reset the token bucket after ttl
redis.call("setex", timestamp_key, ttl, now)

return { allowed, new_tokens , ttl}