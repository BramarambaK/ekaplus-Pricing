import json
from PayloadHelper import executePricing

def lambda_handler(event, context):
    
	auth = ""
	tag= "Power"
	tenant = ""
	payload_json = event
	result = executePricing(auth, payload_json, tag, tenant)
	
	return result
