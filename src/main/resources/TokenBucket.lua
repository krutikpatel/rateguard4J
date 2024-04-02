
-- keys to redis store
local user_token_bucket = KEYS[1]    -- the key for the tokens, for the one who is requesting
local user_bucket_refresh_timestamp = KEYS[2] -- the key for the timestamp. timestamp when the token bucket was last refreshed

-- args: rate, capacity, current time, and requested tokens
local rate = tonumber(ARGV[1])      -- the rate at which the bucket is refilled
local capacity = tonumber(ARGV[2])  -- the maximum number of tokens that the bucket can hold
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4]) -- the number of tokens that the user is requesting

-- local fill_time = capacity/rate
local ttl = rate -- same as rate. | math.floor(fill_time*2) -- the time to live for the token bucket and timestamp

-- initialize the bucket and timestamp if they don't exist
local available_tokens = tonumber(redis.call("get", user_token_bucket))
if available_tokens == nil then
    available_tokens = capacity
end

local last_refreshed = tonumber(redis.call("get", user_bucket_refresh_timestamp))
if last_refreshed == nil then
  last_refreshed = 0
end

-- assuming bucket is emptied at rate, calculate the number of tokens that should be in the bucket now
-- why dont we maintain the actual count?
--[[
local delta = math.max(0, now-last_refreshed)
local filled_tokens = math.min(capacity, available_tokens+(delta*rate))
local allowed = filled_tokens >= requested
local new_tokens = filled_tokens
if allowed then
  new_tokens = filled_tokens - requested
end
]]

local allowed = available_tokens >= requested
local new_tokens = 0
if allowed then
    new_tokens = available_tokens - requested
end

redis.call("setex", user_token_bucket, ttl, new_tokens)
redis.call("setex", user_bucket_refresh_timestamp, ttl, now)

return { allowed, new_tokens }